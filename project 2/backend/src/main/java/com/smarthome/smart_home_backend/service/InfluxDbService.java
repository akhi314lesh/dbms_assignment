package com.smarthome.smart_home_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class InfluxDbService {

    private static final Logger log = LoggerFactory.getLogger(InfluxDbService.class);

    static {
        // A reused keep-alive connection to InfluxDB stalls ~40 ms per request on this setup (request headers and
        // body go out as separate TCP segments and wait on a delayed ACK through Docker's port forwarder), which
        // added ~40 ms to every measured InfluxDB query. Closing idle connections gives each request a fresh
        // connection: ~1 ms to open on localhost instead of the 40 ms stall. Must be set before the first
        // java.net.http.HttpClient is created; this is the backend's only HttpClient.
        if (System.getProperty("jdk.httpclient.keepalive.timeout") == null) {
            System.setProperty("jdk.httpclient.keepalive.timeout", "0");
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final HttpClient httpClient;

    private final String influxUrl;
    private final String influxToken;
    private final String influxOrg;
    private final String influxBucket;
    private final String influxContainer;

    public InfluxDbService(
            JdbcTemplate jdbcTemplate,
            @Value("${influxdb.url:http://localhost:8086}") String influxUrl,
            @Value("${influxdb.token:da3_super_secret_sensor_telemetry_token_123456789}") String influxToken,
            @Value("${influxdb.org:smarthome_org}") String influxOrg,
            @Value("${influxdb.bucket:sensor_telemetry}") String influxBucket,
            @Value("${influxdb.container:da3-influxdb-server}") String influxContainer) {
        this.jdbcTemplate = jdbcTemplate;
        this.influxUrl = influxUrl.replaceAll("/$", "");
        this.influxToken = influxToken;
        this.influxOrg = influxOrg;
        this.influxBucket = influxBucket;
        this.influxContainer = influxContainer;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)   // InfluxDB speaks HTTP/1.1; skip the h2c upgrade attempt
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    /** Start of the Flux range used for the 120 sample readings (they are dated 2026-09-17, older than -30d). */
    public static final String ALL_TIME = "1970-01-01T00:00:00Z";

    /**
     * Check InfluxDB server health status. Reports healthy=false (with the reason) when InfluxDB is
     * not reachable — no simulated "standby" engine.
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        response.put("url", influxUrl);
        response.put("org", influxOrg);
        response.put("bucket", influxBucket);
        try {
            Map<String, Object> h = healthStrict();
            response.put("healthy", true);
            response.put("mode", "Live InfluxDB " + h.get("version") + " (:8086)");
            response.put("version", h.get("version"));
            response.put("statusCode", 200);
            response.put("latencyMs", Math.max(1, System.currentTimeMillis() - start));
        } catch (Exception e) {
            response.put("healthy", false);
            response.put("mode", "InfluxDB offline");
            response.put("statusCode", 503);
            response.put("latencyMs", System.currentTimeMillis() - start);
            response.put("error", e.getMessage());
        }
        return response;
    }

    /**
     * Execute an arbitrary Flux query. On failure returns success=false with InfluxDB's own error
     * message (no simulated rows).
     */
    public Map<String, Object> executeFlux(String fluxQuery) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (fluxQuery == null || fluxQuery.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "Flux query string cannot be empty.");
            result.put("columns", List.of());
            result.put("rows", List.of());
            result.put("rowCount", 0);
            return result;
        }
        long start = System.nanoTime();
        try {
            List<Map<String, String>> parsed = FluxCsvParser.parse(queryCsvStrict(fluxQuery));
            double ms = (System.nanoTime() - start) / 1_000_000.0;
            LinkedHashSet<String> columns = new LinkedHashSet<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Map<String, String> r : parsed) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (var e : r.entrySet()) {
                    if (e.getKey().equals("table") || e.getKey().equals("result")) continue;
                    columns.add(e.getKey());
                    row.put(e.getKey(), e.getValue());
                }
                rows.add(row);
            }
            result.put("success", true);
            result.put("columns", new ArrayList<>(columns));
            result.put("rows", rows);
            result.put("rowCount", rows.size());
            result.put("executionTimeMs", Math.round(ms * 100) / 100.0);
            result.put("query", fluxQuery);
            result.put("message", rows.isEmpty()
                    ? "Query ran in " + Math.round(ms) + " ms but returned no rows (is there data in this time range?)."
                    : "Flux executed successfully in " + Math.round(ms) + " ms.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("columns", List.of());
            result.put("rows", List.of());
            result.put("rowCount", 0);
            result.put("executionTimeMs", Math.round((System.nanoTime() - start) / 10_000.0) / 100.0);
            result.put("query", fluxQuery);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** Write Line Protocol to InfluxDB. On failure returns success=false with the reason. */
    public Map<String, Object> writeLineProtocol(String lineProtocol) {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            writeStrict(lineProtocol == null ? "" : lineProtocol, "ns");
            long latency = Math.max(1, System.currentTimeMillis() - start);
            result.put("success", true);
            result.put("statusCode", 204);
            result.put("executionTimeMs", latency);
            result.put("message", "Successfully ingested points into InfluxDB in " + latency + " ms.");
        } catch (Exception e) {
            result.put("success", false);
            result.put("statusCode", 503);
            result.put("executionTimeMs", System.currentTimeMillis() - start);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * Raw readings of one device from measurement sensor_reading, for charting.
     * timeRange: a Flux duration such as "-7d", or "all" (default) for every reading.
     */
    public Map<String, Object> getTelemetryPoints(Long deviceId, String timeRange) {
        String range = (timeRange == null || timeRange.isBlank() || "all".equalsIgnoreCase(timeRange)) ? ALL_TIME : timeRange.trim();
        if (!range.matches("-?\\d+[smhdwy]|\\d{4}-\\d{2}-\\d{2}T[0-9:.]+Z")) {
            range = ALL_TIME;
        }
        String devFilter = (deviceId != null) ? " and r[\"device_id\"] == \"" + deviceId + "\"" : "";
        String flux = "from(bucket: \"" + influxBucket + "\")\n"
                + "  |> range(start: " + range + ")\n"
                + "  |> filter(fn: (r) => r[\"_measurement\"] == \"sensor_reading\"" + devFilter + ")\n"
                + "  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n"
                + "  |> sort(columns: [\"_time\"])";

        Map<String, Object> queryRes = executeFlux(flux);
        List<Map<String, Object>> formattedPoints = new ArrayList<>();
        if (Boolean.TRUE.equals(queryRes.get("success"))) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) queryRes.get("rows");
            for (Map<String, Object> r : rows) {
                Map<String, Object> point = new HashMap<>();
                point.put("time", r.get("_time"));
                point.put("value", r.get("_value"));
                point.put("deviceId", r.get("device_id"));
                point.put("deviceName", r.get("device_name"));
                point.put("unit", r.get("unit") != null ? r.get("unit") : "°C");
                formattedPoints.add(point);
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", queryRes.get("success"));
        response.put("points", formattedPoints);
        response.put("count", formattedPoints.size());
        response.put("executionTimeMs", queryRes.get("executionTimeMs"));
        response.put("message", formattedPoints.isEmpty() && Boolean.TRUE.equals(queryRes.get("success"))
                ? "No readings for this device in InfluxDB yet. Click \"Ingest Sample\" to copy Oracle's SENSOR_READINGS into InfluxDB."
                : queryRes.get("message"));
        return response;
    }

    /** Devices that have readings in Oracle SENSOR_READINGS (the data the hub works with). */
    public List<Map<String, Object>> devicesWithReadings() {
        return jdbcTemplate.query(
                "SELECT d.device_id, d.device_name, d.device_subtype, COUNT(*) AS cnt "
                        + "FROM sensor_readings s JOIN devices d ON d.device_id = s.device_id "
                        + "GROUP BY d.device_id, d.device_name, d.device_subtype ORDER BY d.device_id",
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", rs.getLong(1));
                    m.put("name", rs.getString(2));
                    m.put("subtype", rs.getString(3));
                    m.put("readings", rs.getLong(4));
                    return m;
                });
    }

    /**
     * Side-by-side benchmark on the SAME question: hourly average of one device's readings.
     * Oracle reads SENSOR_READINGS, InfluxDB reads measurement sensor_reading (filled by /migrate).
     * One untimed warm-up, then 5 timed runs each; reports the average and whether results match.
     * For the full benchmark suite use the comparison dashboard (:5175, /api/compare/**).
     */
    public Map<String, Object> runBenchmark(Long deviceId) {
        long dev = (deviceId != null) ? deviceId : 4L;
        final int runs = 5;
        String oracleSql = "SELECT TRUNC(reading_time, 'HH') AS time_bucket, ROUND(AVG(value), 2) AS avg_value "
                + "FROM sensor_readings WHERE device_id = ? GROUP BY TRUNC(reading_time, 'HH') ORDER BY time_bucket";
        String flux = "from(bucket: \"" + influxBucket + "\")\n"
                + "  |> range(start: " + ALL_TIME + ")\n"
                + "  |> filter(fn: (r) => r[\"_measurement\"] == \"sensor_reading\" and r[\"device_id\"] == \"" + dev + "\")\n"
                + "  |> filter(fn: (r) => r[\"_field\"] == \"value\")\n"
                + "  |> aggregateWindow(every: 1h, fn: mean, createEmpty: false, timeSrc: \"_start\")\n"
                + "  |> keep(columns: [\"_time\", \"_value\"])";

        List<Map<String, Object>> oracleRows = List.of();
        String oracleError = null;
        double oracleTotal = 0;
        try {
            jdbcTemplate.queryForList(oracleSql, dev);                         // warm-up
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                oracleRows = jdbcTemplate.queryForList(oracleSql, dev);
                oracleTotal += (System.nanoTime() - t0) / 1_000_000.0;
            }
        } catch (Exception e) {
            oracleError = "Oracle query failed: " + rootMessage(e);
        }

        List<Map<String, String>> influxRows = List.of();
        String influxError = null;
        double influxTotal = 0;
        try {
            queryCsvStrict(flux);                                            // warm-up
            for (int i = 0; i < runs; i++) {
                long t0 = System.nanoTime();
                influxRows = FluxCsvParser.parse(queryCsvStrict(flux));
                influxTotal += (System.nanoTime() - t0) / 1_000_000.0;
            }
        } catch (Exception e) {
            influxError = rootMessage(e);
        }

        Double oracleMs = oracleError == null ? Math.round(oracleTotal / runs * 100) / 100.0 : null;
        Double influxMs = influxError == null ? Math.round(influxTotal / runs * 100) / 100.0 : null;

        // Compare the hourly averages returned by both databases
        Map<String, Double> o = new TreeMap<>();
        for (Map<String, Object> r : oracleRows) {
            Object tb = r.get("TIME_BUCKET");
            if (tb instanceof java.util.Date d && r.get("AVG_VALUE") instanceof Number n) {
                String key = new java.sql.Timestamp(d.getTime()).toLocalDateTime().toString();
                o.put(key.length() >= 16 ? key.substring(0, 16) : key, n.doubleValue());
            }
        }
        Map<String, Double> f = new TreeMap<>();
        for (Map<String, String> r : influxRows) {
            String t = r.get("_time");
            if (t != null && t.length() >= 16 && r.get("_value") != null && !r.get("_value").isBlank()) {
                f.put(t.substring(0, 16), Double.parseDouble(r.get("_value")));
            }
        }
        boolean resultsMatch = !o.isEmpty() && o.keySet().equals(f.keySet())
                && o.keySet().stream().allMatch(k -> Math.abs(o.get(k) - f.get(k)) <= 0.006);

        Map<String, Object> benchmark = new LinkedHashMap<>();
        benchmark.put("targetDeviceId", dev);
        benchmark.put("timestamp", java.time.Instant.now().toString());
        benchmark.put("timedRuns", runs);

        Map<String, Object> oracleCard = new LinkedHashMap<>();
        oracleCard.put("database", "Oracle Database 21c XE");
        oracleCard.put("model", "Relational (B-tree primary key)");
        oracleCard.put("queryLanguage", "SQL");
        oracleCard.put("query", oracleSql.replace("?", String.valueOf(dev)));
        oracleCard.put("latencyMs", oracleMs);
        oracleCard.put("rowsReturned", oracleError == null ? oracleRows.size() : null);
        oracleCard.put("dataPayload", oracleRows.subList(0, Math.min(20, oracleRows.size())));
        oracleCard.put("error", oracleError);

        Map<String, Object> influxCard = new LinkedHashMap<>();
        influxCard.put("database", "InfluxDB 2.x");
        influxCard.put("model", "Time-Structured Merge Tree (TSM)");
        influxCard.put("queryLanguage", "Flux");
        influxCard.put("query", flux);
        influxCard.put("latencyMs", influxMs);
        influxCard.put("rowsReturned", influxError == null ? influxRows.size() : null);
        influxCard.put("dataPayload", influxRows.subList(0, Math.min(20, influxRows.size())));
        influxCard.put("error", influxError);

        benchmark.put("oracle", oracleCard);
        benchmark.put("influx", influxCard);
        benchmark.put("resultsMatch", resultsMatch);

        String summary;
        if (oracleMs != null && influxMs != null) {
            boolean influxFaster = influxMs < oracleMs;
            double ratio = Math.max(oracleMs, influxMs) / Math.max(Math.min(oracleMs, influxMs), 0.01);
            benchmark.put("speedupRatio", Math.round(ratio * 10.0) / 10.0);
            benchmark.put("winner", influxFaster ? "INFLUX" : "ORACLE");
            summary = String.format(java.util.Locale.ROOT,
                    "Hourly average for device %d: Oracle %.2f ms vs InfluxDB %.2f ms (average of %d runs) — %s was %.1fx faster. %s",
                    dev, oracleMs, influxMs, runs, influxFaster ? "InfluxDB" : "Oracle", ratio,
                    f.isEmpty() ? "InfluxDB returned no rows: click \"Ingest Sample\" first."
                            : resultsMatch ? "Both returned the same " + o.size() + " hourly averages." : "The results differ — re-run \"Ingest Sample\".");
        } else {
            benchmark.put("speedupRatio", null);
            benchmark.put("winner", null);
            summary = "Benchmark incomplete: " + (oracleError != null ? oracleError : "") + (influxError != null ? " " + influxError : "");
        }
        benchmark.put("summary", summary.trim());
        return benchmark;
    }

    /**
     * Copies all Oracle SENSOR_READINGS into InfluxDB measurement sensor_reading. Idempotent: the same
     * (tags, timestamp) overwrites the existing point instead of duplicating it.
     */
    public Map<String, Object> migrateSampleReadings() {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            String sql = "SELECT s.device_id, s.reading_id, s.value, s.reading_time, d.device_name, d.device_subtype "
                    + "FROM sensor_readings s JOIN devices d ON s.device_id = d.device_id ORDER BY s.reading_time ASC";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) {
                summary.put("success", false);
                summary.put("message", "No records found in Oracle SENSOR_READINGS table.");
                return summary;
            }
            StringBuilder lp = new StringBuilder();
            for (Map<String, Object> row : rows) {
                long deviceId = ((Number) row.get("DEVICE_ID")).longValue();
                String devName = escapeTag(String.valueOf(row.get("DEVICE_NAME")));
                String devSubtype = escapeTag(String.valueOf(row.get("DEVICE_SUBTYPE")));
                double value = ((Number) row.get("VALUE")).doubleValue();
                java.sql.Timestamp ts = (java.sql.Timestamp) row.get("READING_TIME");
                // Oracle TIMESTAMP has no zone; treat it as UTC so Flux times equal Oracle wall-clock times.
                long epochNs = ts.toLocalDateTime().toEpochSecond(java.time.ZoneOffset.UTC) * 1_000_000_000L;
                String unit = "TEMPERATURE_SENSOR".equals(row.get("DEVICE_SUBTYPE")) ? "CELSIUS" : "STATE";
                lp.append(String.format(java.util.Locale.ROOT,
                        "sensor_reading,device_id=%d,device_name=%s,device_subtype=%s,unit=%s value=%.2f %d\n",
                        deviceId, devName, devSubtype, unit, value, epochNs));
            }
            // Re-sync: remove earlier copies first so the InfluxDB data always equals Oracle's (no duplicates
            // from older migrations that used different tags or timestamps).
            deleteMeasurementStrict("sensor_reading");
            writeStrict(lp.toString(), "ns");
            summary.put("success", true);
            summary.put("recordsIngested", rows.size());
            summary.put("message", "✓ Copied " + rows.size() + " Oracle sensor readings into InfluxDB bucket '" + influxBucket + "' (measurement sensor_reading re-synced).");
        } catch (Exception e) {
            summary.put("success", false);
            summary.put("recordsIngested", 0);
            summary.put("message", "Migration failed: " + rootMessage(e));
        }
        return summary;
    }

    /**
     * Live consistency checks between Oracle SENSOR_READINGS and InfluxDB measurement sensor_reading.
     * Every check is computed now, from both databases.
     */
    public Map<String, Object> verifyMigration() {
        List<Map<String, Object>> checks = new ArrayList<>();
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            Map<String, Object> oTot = jdbcTemplate.queryForMap(
                    "SELECT COUNT(*) AS cnt, ROUND(AVG(value), 4) AS avg_v, MIN(value) AS min_v, MAX(value) AS max_v FROM sensor_readings");
            List<Map<String, Object>> oDev = jdbcTemplate.queryForList(
                    "SELECT device_id, COUNT(*) AS cnt, ROUND(AVG(value), 4) AS avg_v FROM sensor_readings GROUP BY device_id ORDER BY device_id");
            Map<String, Object> oLast = jdbcTemplate.queryForMap(
                    "SELECT MAX(value) KEEP (DENSE_RANK LAST ORDER BY reading_time) AS v, MAX(reading_time) AS t FROM sensor_readings WHERE device_id = 4");

            String base = "from(bucket: \"" + influxBucket + "\")\n  |> range(start: " + ALL_TIME + ")\n"
                    + "  |> filter(fn: (r) => r._measurement == \"sensor_reading\" and r._field == \"value\")";
            Map<String, Double> fTot = new HashMap<>();
            for (var r : FluxCsvParser.parse(queryCsvStrict("data = " + base + "\n  |> group()\n"
                    + "data |> count() |> yield(name: \"cnt\")\ndata |> mean() |> yield(name: \"avg\")\n"
                    + "data |> min() |> yield(name: \"min\")\ndata |> max() |> yield(name: \"max\")"))) {
                fTot.put(r.get("result"), Double.parseDouble(r.get("_value")));
            }
            Map<String, double[]> fDev = new TreeMap<>();
            for (var r : FluxCsvParser.parse(queryCsvStrict("data = " + base + "\n  |> group(columns: [\"device_id\"])\n"
                    + "data |> count() |> yield(name: \"cnt\")\ndata |> mean() |> yield(name: \"avg\")"))) {
                double[] a = fDev.computeIfAbsent(r.get("device_id"), k -> new double[2]);
                if ("cnt".equals(r.get("result"))) a[0] = Double.parseDouble(r.get("_value"));
                else a[1] = Double.parseDouble(r.get("_value"));
            }
            List<Map<String, String>> fLast = FluxCsvParser.parse(queryCsvStrict(
                    base.replace("r._field == \"value\"", "r._field == \"value\" and r.device_id == \"4\"") + "\n  |> last()"));

            long oCnt = ((Number) oTot.get("CNT")).longValue();
            double fCnt = fTot.getOrDefault("cnt", 0.0);
            checks.add(check("Total reading count", oCnt + " rows", (long) fCnt + " points", oCnt == (long) fCnt));
            if (oCnt > 0) {
                double oAvg = ((Number) oTot.get("AVG_V")).doubleValue();
                checks.add(check("Overall average value", fmt(oAvg), fmt(fTot.getOrDefault("avg", Double.NaN)),
                        Math.abs(oAvg - fTot.getOrDefault("avg", Double.NaN)) < 0.001));
                double oMin = ((Number) oTot.get("MIN_V")).doubleValue();
                double oMax = ((Number) oTot.get("MAX_V")).doubleValue();
                checks.add(check("Minimum and maximum value", fmt(oMin) + " / " + fmt(oMax),
                        fmt(fTot.getOrDefault("min", Double.NaN)) + " / " + fmt(fTot.getOrDefault("max", Double.NaN)),
                        Math.abs(oMin - fTot.getOrDefault("min", Double.NaN)) < 0.001 && Math.abs(oMax - fTot.getOrDefault("max", Double.NaN)) < 0.001));
            }
            int devOk = 0;
            for (Map<String, Object> d : oDev) {
                String id = String.valueOf(((Number) d.get("DEVICE_ID")).longValue());
                double[] a = fDev.get(id);
                if (a != null && (long) a[0] == ((Number) d.get("CNT")).longValue()
                        && Math.abs(a[1] - ((Number) d.get("AVG_V")).doubleValue()) < 0.001) devOk++;
            }
            checks.add(check("Per-device count and average", oDev.size() + " devices", devOk + " devices identical", devOk == oDev.size() && !oDev.isEmpty()));
            if (oLast.get("V") != null) {
                String oV = fmt(((Number) oLast.get("V")).doubleValue());
                String oT = ((java.sql.Timestamp) oLast.get("T")).toLocalDateTime().toString().replace('T', ' ');
                String fV = fLast.isEmpty() ? "—" : fmt(Double.parseDouble(fLast.get(0).get("_value")));
                String fT = fLast.isEmpty() ? "" : fLast.get(0).get("_time").replace('T', ' ').replace("Z", "");
                checks.add(check("Latest reading of device 4", oV + " at " + oT, fV + (fT.isEmpty() ? "" : " at " + fT),
                        oV.equals(fV) && fT.startsWith(oT.length() >= 16 ? oT.substring(0, 16) : oT)));
            }
        } catch (Exception e) {
            out.put("error", rootMessage(e));
        }
        long passed = checks.stream().filter(c -> Boolean.TRUE.equals(c.get("passed"))).count();
        out.put("checks", checks);
        out.put("passed", passed);
        out.put("total", checks.size());
        out.put("timestamp", java.time.Instant.now().toString());
        return out;
    }

    private static Map<String, Object> check(String name, String oracle, String influx, boolean passed) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("oracle", oracle);
        m.put("influx", influx);
        m.put("passed", passed);
        return m;
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "—" : String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static String escapeTag(String s) {
        return s.replace("\\", "\\\\").replace(",", "\\,").replace("=", "\\=").replace(" ", "\\ ");
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = (e instanceof IllegalStateException && e.getMessage() != null) ? e.getMessage()
                : (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        return m.replaceAll("\\s+", " ").trim();
    }

    // ──────────────────────────────────────────────────────────────────────
    // STRICT API (used by the Oracle vs InfluxDB comparison engine).
    // These methods never fall back to simulated data: if InfluxDB is down or
    // a query is invalid they throw, so benchmark numbers are always real.
    // ──────────────────────────────────────────────────────────────────────

    public String getBucket() { return influxBucket; }
    public String getOrg() { return influxOrg; }
    public String getUrl() { return influxUrl; }

    /** Real health probe: returns the parsed /health JSON fields or throws. */
    public Map<String, Object> healthStrict() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(influxUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(3))
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = res.body() == null ? "" : res.body();
            if (res.statusCode() != 200 || !body.contains("\"pass\"")) {
                throw new IllegalStateException("InfluxDB /health returned HTTP " + res.statusCode() + ": " + abbreviate(body));
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", "pass");
            out.put("version", extractJsonString(body, "version"));
            out.put("url", influxUrl);
            out.put("org", influxOrg);
            out.put("bucket", influxBucket);
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB is not reachable at " + influxUrl + " (" + e.getClass().getSimpleName()
                    + (e.getMessage() != null ? ": " + e.getMessage() : "") + ")", e);
        }
    }

    /** Executes a Flux query and returns the raw annotated CSV body, or throws with InfluxDB's error message. */
    public String queryCsvStrict(String flux) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(influxUrl + "/api/v2/query?org=" + enc(influxOrg)))
                    .header("Authorization", "Token " + influxToken)
                    .header("Accept", "application/csv")
                    .header("Content-Type", "application/vnd.flux")
                    .POST(HttpRequest.BodyPublishers.ofString(flux, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(120))
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = res.body() == null ? "" : res.body();
            if (res.statusCode() != 200) {
                throw new IllegalStateException("InfluxDB query failed (HTTP " + res.statusCode() + "): " + influxErrorMessage(body));
            }
            // Flux can also report runtime errors inside a 200 CSV body as an "error" table.
            if (body.startsWith("#datatype,string,string") && body.contains(",error,reference")) {
                throw new IllegalStateException("InfluxDB query error: " + abbreviate(body));
            }
            return body;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB query could not be executed: " + e.getMessage(), e);
        }
    }

    /** Writes line protocol (precision = "s", "ms", "us" or "ns") or throws. */
    public void writeStrict(String lineProtocol, String precision) {
        try {
            String uri = influxUrl + "/api/v2/write?org=" + enc(influxOrg) + "&bucket=" + enc(influxBucket) + "&precision=" + precision;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Authorization", "Token " + influxToken)
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(lineProtocol, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(120))
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new IllegalStateException("InfluxDB write failed (HTTP " + res.statusCode() + "): " + influxErrorMessage(res.body()));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB write could not be executed: " + e.getMessage(), e);
        }
    }

    /** Deletes every point of a measurement in the configured bucket, or throws. */
    public void deleteMeasurementStrict(String measurement) {
        try {
            String uri = influxUrl + "/api/v2/delete?org=" + enc(influxOrg) + "&bucket=" + enc(influxBucket);
            String json = "{\"start\":\"1970-01-01T00:00:00Z\",\"stop\":\"2100-01-01T00:00:00Z\","
                    + "\"predicate\":\"_measurement=\\\"" + measurement + "\\\"\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Authorization", "Token " + influxToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(120))
                    .build();
            HttpResponse<String> res = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                throw new IllegalStateException("InfluxDB delete failed (HTTP " + res.statusCode() + "): " + influxErrorMessage(res.body()));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB delete could not be executed: " + e.getMessage(), e);
        }
    }

    /**
     * On-disk size of the configured bucket, read from InfluxDB's own Prometheus /metrics endpoint:
     * storage_shard_disk_size (compressed TSM files) and storage_wal_size (write-ahead log not yet compacted).
     */
    public Map<String, Object> bucketStorageStrict() {
        try {
            HttpRequest bucketReq = HttpRequest.newBuilder()
                    .uri(URI.create(influxUrl + "/api/v2/buckets?org=" + enc(influxOrg) + "&name=" + enc(influxBucket)))
                    .header("Authorization", "Token " + influxToken)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> bres = httpClient.send(bucketReq, HttpResponse.BodyHandlers.ofString());
            if (bres.statusCode() != 200) {
                throw new IllegalStateException("InfluxDB bucket lookup failed (HTTP " + bres.statusCode() + "): " + influxErrorMessage(bres.body()));
            }
            java.util.regex.Matcher bm = java.util.regex.Pattern
                    .compile("\"id\"\\s*:\\s*\"([0-9a-f]{16})\"[^{}]*?\"name\"\\s*:\\s*\"" + java.util.regex.Pattern.quote(influxBucket) + "\"")
                    .matcher(bres.body());
            if (!bm.find()) throw new IllegalStateException("Bucket '" + influxBucket + "' not found in InfluxDB.");
            String bucketId = bm.group(1);

            HttpRequest metricsReq = HttpRequest.newBuilder()
                    .uri(URI.create(influxUrl + "/metrics"))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> mres = httpClient.send(metricsReq, HttpResponse.BodyHandlers.ofString());
            if (mres.statusCode() != 200) {
                throw new IllegalStateException("InfluxDB /metrics returned HTTP " + mres.statusCode());
            }
            double tsm = 0, wal = 0;
            int shards = 0;
            String label = "bucket=\"" + bucketId + "\"";
            for (String line : mres.body().split("\n")) {
                if (line.startsWith("#") || !line.contains(label)) continue;
                int sp = line.lastIndexOf(' ');
                if (sp < 0) continue;
                double v;
                try { v = Double.parseDouble(line.substring(sp + 1).trim()); } catch (NumberFormatException e) { continue; }
                if (line.startsWith("storage_shard_disk_size{")) { tsm += v; shards++; }
                else if (line.startsWith("storage_wal_size{")) wal += v;
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("bucketId", bucketId);
            out.put("tsmBytes", (long) tsm);
            out.put("walBytes", (long) wal);
            out.put("shards", shards);
            out.put("diskBytes", bucketDiskBytes(bucketId));   // null when Docker cannot be asked
            return out;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("InfluxDB storage size could not be read: " + e.getMessage(), e);
        }
    }

    /**
     * Disk blocks actually used by the bucket (data + index + series file + WAL), measured with {@code du -sk}
     * inside the InfluxDB container. Block usage, not apparent size: the series file is a sparse, preallocated
     * file whose apparent size (32 MB) is far larger than what it occupies.
     */
    private Long bucketDiskBytes(String bucketId) {
        try {
            Process p = new ProcessBuilder("docker", "exec", influxContainer, "du", "-sk",
                    "/var/lib/influxdb2/engine/data/" + bucketId, "/var/lib/influxdb2/engine/wal/" + bucketId)
                    .redirectErrorStream(true).start();
            String text = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(15, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
                log.warn("du in container {} failed: {}", influxContainer, abbreviate(text));
                return null;
            }
            long kb = 0;
            for (String line : text.split("\n")) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) kb += Long.parseLong(parts[0]);
            }
            return kb * 1024;
        } catch (Exception e) {
            log.warn("Could not measure InfluxDB disk usage via docker exec: {}", e.getMessage());
            return null;
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String influxErrorMessage(String body) {
        if (body == null || body.isBlank()) return "(empty response)";
        String msg = extractJsonString(body, "message");
        return msg != null ? msg : abbreviate(body);
    }

    private static String extractJsonString(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                .matcher(json);
        return m.find() ? m.group(1).replace("\\\"", "\"") : null;
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 300 ? t.substring(0, 300) + "…" : t;
    }
}
