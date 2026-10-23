package com.smarthome.smart_home_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Oracle vs InfluxDB comparison engine (DA3 · T1).
 *
 * How a fair comparison is guaranteed:
 *  1. Identical data — {@link #seed} generates ONE deterministic telemetry dataset and writes the exact
 *     same readings to Oracle (table SENSOR_BENCHMARK) and InfluxDB (measurement sensor_benchmark).
 *  2. Identical questions — every query type has an Oracle SQL and an InfluxDB Flux version that answer
 *     the same question over the same half-open time range [start, stop).
 *  3. Real timings — wall-clock time (System.nanoTime) measured in the backend for the full round trip:
 *     send query → database executes → all rows received and materialised in Java. One untimed warm-up run
 *     per side, then N timed iterations, alternating which database goes first.
 *  4. Verified answers — both result sets are normalised and compared row by row (with a small tolerance
 *     for floating point), so the dashboard can prove both engines returned the same answer.
 *
 * No simulated / fallback numbers are ever produced here: if a database is down, its side reports an error.
 */
@Service
public class CompareService {

    private static final Logger log = LoggerFactory.getLogger(CompareService.class);

    public static final String ORACLE_TABLE = "SENSOR_BENCHMARK";
    public static final String ORACLE_WRITE_TABLE = "SENSOR_BENCHMARK_WRITE";
    public static final String MEASUREMENT = "sensor_benchmark";
    public static final String WRITE_MEASUREMENT = "sensor_benchmark_write";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter SQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int ORACLE_BATCH = 5000;
    private static final int INFLUX_BATCH = 10000;
    private static final long MAX_SEED_ROWS = 5_000_000L;
    private static final int MAX_SEED_DAYS = 365;

    private final JdbcTemplate jdbc;
    private final InfluxDbService influx;
    private final ReentrantLock lock = new ReentrantLock();
    private final Deque<Map<String, Object>> history = new ConcurrentLinkedDeque<>();
    private volatile Map<String, Object> lastSeed;
    private volatile boolean schemaReady = false;

    public CompareService(DataSource dataSource, InfluxDbService influx) {
        this.jdbc = new JdbcTemplate(dataSource);
        // Oracle's JDBC default fetch size is only 10 rows per round trip, which would unfairly penalise
        // Oracle on large result sets. 1000 is a standard production setting.
        this.jdbc.setFetchSize(1000);
        this.influx = influx;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Query catalogue
    // ═════════════════════════════════════════════════════════════════════

    public record QueryDef(String key, String label, String category, String scope, String description,
                           int defaultRangeHours, String align, String explanation, boolean write) {}

    private static final Map<String, QueryDef> CATALOG = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        add(new QueryDef("RAW_RANGE", "Raw time-range scan", "Read", "One device",
                "Return every raw reading of one sensor inside a time window, ordered by time.",
                24, "HOUR",
                "Oracle walks the composite primary-key B-tree (device_id, reading_time) and fetches each row; "
                        + "InfluxDB reads one series whose points are already stored contiguously in time order inside "
                        + "compressed TSM blocks. With small windows both are fast; the cost is dominated by transferring rows.",
                false));
        add(new QueryDef("WINDOW_AGGREGATE", "Hourly window average", "Aggregate", "One device",
                "Downsample one sensor to hourly averages (GROUP BY hour vs aggregateWindow).",
                168, "HOUR",
                "Oracle must read every row, compute TRUNC(reading_time,'HH') per row and hash-group the result. "
                        + "InfluxDB's aggregateWindow() streams the time-ordered series and closes each window as it goes, "
                        + "which is exactly the access pattern of IoT dashboards.",
                false));
        add(new QueryDef("DEVICE_SUMMARY", "Per-device statistics", "Aggregate", "All devices",
                "COUNT / AVG / MIN / MAX per device over the time window.",
                168, "HOUR",
                "A classic GROUP BY. Oracle scans the time index or table and aggregates in one pass. InfluxDB runs "
                        + "count(), mean(), min() and max() per series; several of these are pushed down into the storage engine.",
                false));
        add(new QueryDef("LATEST_PER_DEVICE", "Latest reading per device", "Lookup", "All devices",
                "Most recent value of every sensor (\"current state\" panel).",
                168, "HOUR",
                "Oracle uses MAX(...) KEEP (DENSE_RANK LAST ORDER BY reading_time) per device, which still visits the rows "
                        + "in the window. InfluxDB's last() is a storage push-down: it reads only the newest block of each series.",
                false));
        add(new QueryDef("THRESHOLD_ALERTS", "Threshold breach detection", "Filter + aggregate", "All devices",
                "Count readings above a threshold per device and report the peak value (alerting).",
                168, "HOUR",
                "Oracle filters with a WHERE predicate on value (no index on value, so it scans the window). "
                        + "InfluxDB must also decode every point to test the value, because tags are indexed but field values are not.",
                false));
        add(new QueryDef("DAILY_DOWNSAMPLE", "Daily downsampling (all devices)", "Aggregate", "All devices",
                "Daily average per device — typical long-term retention / reporting query.",
                0, "DAY",
                "Both engines read the full dataset. Oracle groups by (device_id, TRUNC(day)); InfluxDB applies "
                        + "aggregateWindow(every: 1d) to each series independently, which parallels naturally over series.",
                false));
        add(new QueryDef("GLOBAL_AGGREGATE", "Full-scan aggregate", "Aggregate", "All devices",
                "COUNT / AVG / MIN / MAX across every reading in the window.",
                0, "HOUR",
                "A full scan. Oracle reads all blocks of the table (multi-block reads, very efficient for a single-row "
                        + "result). InfluxDB has to merge every series into one table with group() before aggregating.",
                false));
        add(new QueryDef("BULK_INSERT", "Bulk ingestion (write)", "Write", "All devices",
                "Insert a batch of new sensor readings into each database (JDBC batch INSERT vs Line Protocol).",
                0, "NONE",
                "Oracle performs a transactional INSERT per row: it maintains the primary-key and time indexes, checks the "
                        + "foreign key to DEVICES, writes undo and redo. InfluxDB appends Line Protocol to its write-ahead log and "
                        + "in-memory cache and compresses later, so ingestion is usually its biggest advantage.",
                true));

        ALIASES.put("range_scan", "RAW_RANGE");
        ALIASES.put("RAW_RANGE_SCAN", "RAW_RANGE");
        ALIASES.put("windowed_avg", "WINDOW_AGGREGATE");
        ALIASES.put("device_agg", "DEVICE_SUMMARY");
        ALIASES.put("MULTI_DEVICE_GROUP", "DEVICE_SUMMARY");
        ALIASES.put("top_n", "LATEST_PER_DEVICE");
        ALIASES.put("anomaly_detect", "THRESHOLD_ALERTS");
        ALIASES.put("FULL_SCAN_AGGREGATE", "GLOBAL_AGGREGATE");
        ALIASES.put("WRITE", "BULK_INSERT");
    }

    private static void add(QueryDef d) { CATALOG.put(d.key(), d); }

    public List<Map<String, Object>> queryCatalog() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (QueryDef d : CATALOG.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", d.key());
            m.put("label", d.label());
            m.put("category", d.category());
            m.put("scope", d.scope());
            m.put("description", d.description());
            m.put("defaultRangeHours", d.defaultRangeHours());
            m.put("explanation", d.explanation());
            m.put("write", d.write());
            m.put("needsDevice", "One device".equals(d.scope()));
            out.add(m);
        }
        return out;
    }

    private static QueryDef resolve(String key) {
        if (key == null || key.isBlank()) return CATALOG.get("WINDOW_AGGREGATE");
        String k = ALIASES.getOrDefault(key, ALIASES.getOrDefault(key.toLowerCase(Locale.ROOT), key.toUpperCase(Locale.ROOT)));
        QueryDef d = CATALOG.get(k);
        if (d == null) throw new IllegalArgumentException("Unknown queryType '" + key + "'. Valid values: " + CATALOG.keySet());
        return d;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Health
    // ═════════════════════════════════════════════════════════════════════

    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        try {
            String version = jdbc.execute((ConnectionCallback<String>) con -> {
                var md = con.getMetaData();
                String v = md.getDatabaseProductVersion().split("\n")[0].trim();   // e.g. "Oracle Database 21c Express Edition Release 21.0.0.0.0 - Production"
                return v.startsWith(md.getDatabaseProductName()) ? v : md.getDatabaseProductName() + " " + v;
            });
            out.put("oracle", true);
            out.put("oracleVersion", version);
        } catch (Exception e) {
            out.put("oracle", false);
            out.put("oracleError", rootMessage(e));
        }
        try {
            Map<String, Object> h = influx.healthStrict();
            out.put("influx", true);
            out.put("influxVersion", "InfluxDB " + h.get("version"));
        } catch (Exception e) {
            out.put("influx", false);
            out.put("influxError", rootMessage(e));
        }
        out.put("influxUrl", influx.getUrl());
        out.put("influxBucket", influx.getBucket());
        out.put("timestamp", Instant.now().toString());
        return out;
    }

    private void requireBothUp() {
        Map<String, Object> h = health();
        List<String> problems = new ArrayList<>();
        if (!Boolean.TRUE.equals(h.get("oracle"))) problems.add("Oracle is not reachable: " + h.get("oracleError"));
        if (!Boolean.TRUE.equals(h.get("influx"))) problems.add("InfluxDB is not reachable: " + h.get("influxError"));
        if (!problems.isEmpty()) throw new IllegalStateException(String.join(" | ", problems));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Schema + devices
    // ═════════════════════════════════════════════════════════════════════

    private void ensureSchema() {
        if (schemaReady) return;
        createTableIfMissing(ORACLE_TABLE, "PK_SENSOR_BENCH", "FK_SENSOR_BENCH_DEV", "IDX_SENSOR_BENCH_TIME");
        createTableIfMissing(ORACLE_WRITE_TABLE, "PK_SENSOR_BENCH_W", "FK_SENSOR_BENCH_W_DEV", "IDX_SENSOR_BENCH_W_TIME");
        schemaReady = true;
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = ?", Integer.class, table);
        return n != null && n > 0;
    }

    private void createTableIfMissing(String table, String pk, String fk, String idx) {
        if (tableExists(table)) return;
        log.info("Creating Oracle benchmark table {}", table);
        jdbc.execute("CREATE TABLE " + table + " ("
                + " device_id NUMBER NOT NULL,"
                + " reading_time TIMESTAMP NOT NULL,"
                + " value NUMBER(10,2) NOT NULL,"
                + " CONSTRAINT " + pk + " PRIMARY KEY (device_id, reading_time),"
                + " CONSTRAINT " + fk + " FOREIGN KEY (device_id) REFERENCES DEVICES(device_id))");
        jdbc.execute("CREATE INDEX " + idx + " ON " + table + " (reading_time)");
    }

    public record Device(long id, String name, String subtype) {}

    private List<Device> loadSensorDevices() {
        return jdbc.query("SELECT device_id, device_name, device_subtype FROM DEVICES "
                        + "WHERE device_subtype IN ('TEMPERATURE_SENSOR', 'MOTION_SENSOR') ORDER BY device_id",
                (rs, i) -> new Device(rs.getLong(1), rs.getString(2), rs.getString(3)));
    }

    public List<Map<String, Object>> devices() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Device d : loadSensorDevices()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deviceId", d.id());
            m.put("deviceName", d.name());
            m.put("subtype", d.subtype());
            out.add(m);
        }
        return out;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Dataset generation (identical data in both databases)
    // ═════════════════════════════════════════════════════════════════════

    record Reading(long deviceId, String subtype, LocalDateTime time, BigDecimal value) {}

    static List<Reading> generate(List<Device> devices, LocalDateTime start, LocalDateTime endExclusive, int intervalMinutes, long seedSalt) {
        List<Reading> out = new ArrayList<>();
        for (Device d : devices) {
            Random rnd = new Random(d.id() * 7919L + seedSalt);
            boolean motion = "MOTION_SENSOR".equals(d.subtype());
            double base = 19 + (d.id() % 9);
            double amp = 2.5 + (d.id() % 3);
            for (LocalDateTime t = start; t.isBefore(endExclusive); t = t.plusMinutes(intervalMinutes)) {
                double h = t.getHour() + t.getMinute() / 60.0;
                double v;
                if (motion) {
                    double p = (h >= 7 && h < 23) ? 0.18 : 0.02;
                    v = rnd.nextDouble() < p ? 1.0 : 0.0;
                } else {
                    v = base + amp * Math.sin(2 * Math.PI * (h - 9) / 24.0) + rnd.nextGaussian() * 0.35;
                    if (rnd.nextDouble() < 0.003) v += 6 + rnd.nextDouble() * 4;   // occasional spike
                }
                out.add(new Reading(d.id(), d.subtype(), t, BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP)));
            }
        }
        return out;
    }

    public Map<String, Object> seed(Integer daysIn, Integer intervalIn) {
        int days = clamp(daysIn == null ? 7 : daysIn, 1, MAX_SEED_DAYS);
        int interval = clamp(intervalIn == null ? 5 : intervalIn, 1, 60);
        acquire();
        try {
            requireBothUp();
            ensureSchema();
            List<Device> devices = loadSensorDevices();
            if (devices.isEmpty()) {
                throw new IllegalStateException("No sensor devices found in the DEVICES table. Load database/04_sample_data.sql first.");
            }
            long perDevice = (long) days * 24 * 60 / interval;
            long total = perDevice * devices.size();
            if (total > MAX_SEED_ROWS) {
                throw new IllegalArgumentException("Requested dataset has " + total + " rows; the maximum is " + MAX_SEED_ROWS
                        + ". Use fewer days or a longer interval.");
            }

            LocalDateTime end = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
            LocalDateTime start = end.minusDays(days);
            List<Reading> rows = generate(devices, start, end, interval, days * 31L + interval);

            // 1. Clear both stores
            jdbc.execute("TRUNCATE TABLE " + ORACLE_TABLE);
            influx.deleteMeasurementStrict(MEASUREMENT);

            // 2. Oracle: JDBC batch insert
            long t0 = System.nanoTime();
            oracleInsert(ORACLE_TABLE, rows);
            double oracleMs = msSince(t0);

            // 3. InfluxDB: Line Protocol batches
            t0 = System.nanoTime();
            influxWrite(MEASUREMENT, rows);
            double influxMs = msSince(t0);

            // 4. Give Oracle's optimizer fresh statistics (what a DBA would do after a bulk load)
            try {
                jdbc.execute("BEGIN DBMS_STATS.GATHER_TABLE_STATS(USER, '" + ORACLE_TABLE + "'); END;");
            } catch (Exception e) {
                log.warn("Could not gather Oracle statistics: {}", rootMessage(e));
            }

            Map<String, Object> ds = datasetInfoInternal();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("days", days);
            out.put("intervalMinutes", interval);
            out.put("devices", devices.size());
            out.put("rowsGenerated", rows.size());
            out.put("start", ISO.format(start));
            out.put("end", ISO.format(end));
            out.put("oracleWriteMs", round(oracleMs, 1));
            out.put("influxWriteMs", round(influxMs, 1));
            out.put("oracleRowsPerSec", Math.round(rows.size() / Math.max(oracleMs, 0.001) * 1000));
            out.put("influxRowsPerSec", Math.round(rows.size() / Math.max(influxMs, 0.001) * 1000));
            out.put("writeWinner", oracleMs <= influxMs ? "ORACLE" : "INFLUX");
            out.put("writeSpeedup", round(Math.max(oracleMs, influxMs) / Math.max(Math.min(oracleMs, influxMs), 0.001), 2));
            out.put("dataset", ds);
            out.put("timestamp", Instant.now().toString());
            lastSeed = out;
            return out;
        } finally {
            lock.unlock();
        }
    }

    private void oracleInsert(String table, List<Reading> rows) {
        jdbc.batchUpdate("INSERT INTO " + table + " (device_id, reading_time, value) VALUES (?, ?, ?)",
                rows, ORACLE_BATCH, (ps, r) -> {
                    ps.setLong(1, r.deviceId());
                    ps.setTimestamp(2, Timestamp.valueOf(r.time()));
                    ps.setBigDecimal(3, r.value());
                });
    }

    private void influxWrite(String measurement, List<Reading> rows) {
        StringBuilder sb = new StringBuilder(INFLUX_BATCH * 70);
        int n = 0;
        for (Reading r : rows) {
            sb.append(measurement)
                    .append(",device_id=").append(r.deviceId())
                    .append(",device_type=").append(r.subtype())
                    .append(" value=").append(r.value().toPlainString())
                    .append(' ').append(r.time().toEpochSecond(ZoneOffset.UTC))
                    .append('\n');
            if (++n == INFLUX_BATCH) {
                influx.writeStrict(sb.toString(), "s");
                sb.setLength(0);
                n = 0;
            }
        }
        if (n > 0) influx.writeStrict(sb.toString(), "s");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Dataset information
    // ═════════════════════════════════════════════════════════════════════

    record Bounds(long count, LocalDateTime min, LocalDateTime max, long devices) {}

    private Bounds oracleBounds() {
        if (!schemaReady && !tableExists(ORACLE_TABLE)) return new Bounds(0, null, null, 0);
        return jdbc.queryForObject("SELECT COUNT(*), MIN(reading_time), MAX(reading_time), COUNT(DISTINCT device_id) FROM " + ORACLE_TABLE,
                (rs, i) -> {
                    Timestamp a = rs.getTimestamp(2);
                    Timestamp b = rs.getTimestamp(3);
                    return new Bounds(rs.getLong(1), a == null ? null : a.toLocalDateTime(), b == null ? null : b.toLocalDateTime(), rs.getLong(4));
                });
    }

    private long influxCount(String measurement, String start, String stop) {
        String flux = "from(bucket: \"" + influx.getBucket() + "\")\n"
                + "  |> range(start: " + start + (stop != null ? ", stop: " + stop : "") + ")\n"
                + "  |> filter(fn: (r) => r._measurement == \"" + measurement + "\" and r._field == \"value\")\n"
                + "  |> group()\n"
                + "  |> count()";
        List<Map<String, String>> rows = FluxCsvParser.parse(influx.queryCsvStrict(flux));
        if (rows.isEmpty()) return 0;
        Double v = parseD(rows.get(0).get("_value"));
        return v == null ? 0 : v.longValue();
    }

    public Map<String, Object> datasetInfo() {
        return datasetInfoInternal();
    }

    private Map<String, Object> datasetInfoInternal() {
        Map<String, Object> out = new LinkedHashMap<>();
        Long oracleRows = null;
        Long influxRows = null;
        try {
            Bounds b = oracleBounds();
            oracleRows = b.count();
            out.put("oracleRows", b.count());
            out.put("devices", b.devices());
            out.put("start", b.min() == null ? null : ISO.format(b.min()));
            out.put("end", b.max() == null ? null : ISO.format(b.max()));
        } catch (Exception e) {
            out.put("oracleError", rootMessage(e));
        }
        try {
            influxRows = influxCount(MEASUREMENT, "1970-01-01T00:00:00Z", null);
            out.put("influxRows", influxRows);
        } catch (Exception e) {
            out.put("influxError", rootMessage(e));
        }
        out.put("inSync", oracleRows != null && oracleRows.equals(influxRows) && oracleRows > 0);
        out.put("empty", oracleRows == null || oracleRows == 0);
        out.put("oracleTable", ORACLE_TABLE);
        out.put("influxMeasurement", MEASUREMENT);
        out.put("influxBucket", influx.getBucket());
        out.put("lastSeed", lastSeed == null ? null : stripDataset(lastSeed));
        return out;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Storage footprint of the benchmark dataset
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Space each database uses for the benchmark data. Oracle: allocated bytes of the SENSOR_BENCHMARK table and
     * its indexes (USER_SEGMENTS). InfluxDB: TSM files + write-ahead log of the bucket, from its /metrics endpoint.
     * With {@code settle}, waits (up to 2 minutes) until InfluxDB has compacted its write-ahead log into TSM files,
     * so the compressed size is measured rather than the raw log.
     */
    public Map<String, Object> storage(boolean settle) {
        Map<String, Object> out = new LinkedHashMap<>();
        long rows = 0;
        try {
            rows = oracleBounds().count();
            List<Map<String, Object>> segments = new ArrayList<>();
            long tableBytes = 0, indexBytes = 0;
            if (rows > 0) {
                for (Map<String, Object> s : jdbc.query(
                        "SELECT s.segment_name, s.segment_type, s.bytes FROM user_segments s WHERE s.segment_name = ?"
                                + " UNION ALL SELECT s.segment_name, s.segment_type, s.bytes FROM user_segments s"
                                + " JOIN user_indexes i ON i.index_name = s.segment_name WHERE i.table_name = ?",
                        ROW_MAPPER, ORACLE_TABLE, ORACLE_TABLE)) {
                    long b = num(s.get("bytes")).longValue();
                    boolean index = String.valueOf(s.get("segment_type")).startsWith("INDEX");
                    if (index) indexBytes += b; else tableBytes += b;
                    Map<String, Object> seg = new LinkedHashMap<>();
                    seg.put("name", s.get("segment_name"));
                    seg.put("type", index ? "Index" : "Table");
                    seg.put("bytes", b);
                    segments.add(seg);
                }
            }
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("tableBytes", tableBytes);
            o.put("indexBytes", indexBytes);
            o.put("totalBytes", tableBytes + indexBytes);
            o.put("bytesPerReading", rows > 0 ? round((tableBytes + indexBytes) / (double) rows, 2) : null);
            o.put("segments", segments);
            out.put("oracle", o);
        } catch (Exception e) {
            out.put("oracleError", rootMessage(e));
        }
        try {
            long waited = 0;
            Map<String, Object> s = influx.bucketStorageStrict();
            while (settle && ((Number) s.get("walBytes")).longValue() > 0 && waited < 120_000) {
                Thread.sleep(3000);
                waited += 3000;
                s = influx.bucketStorageStrict();
            }
            long tsm = ((Number) s.get("tsmBytes")).longValue();
            long wal = ((Number) s.get("walBytes")).longValue();
            Long disk = s.get("diskBytes") instanceof Number n ? n.longValue() : null;
            // Index = everything on disk that is not compressed TSM data or WAL: TSI index, series file, field index.
            long index = disk == null ? 0 : Math.max(0, disk - tsm - wal);
            long total = disk == null ? tsm + wal : Math.max(disk, tsm + wal);
            long points = influxCount(MEASUREMENT, "1970-01-01T00:00:00Z", null);
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("tsmBytes", tsm);
            f.put("indexBytes", disk == null ? null : index);
            f.put("walBytes", wal);
            f.put("totalBytes", total);
            f.put("indexMeasured", disk != null);
            f.put("bytesPerReading", points > 0 ? round(total / (double) points, 2) : null);
            f.put("points", points);
            f.put("shards", s.get("shards"));
            f.put("compacted", wal == 0);
            f.put("waitedMs", waited);
            out.put("influx", f);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            out.put("influxError", "Interrupted while waiting for InfluxDB compaction.");
        } catch (Exception e) {
            out.put("influxError", rootMessage(e));
        }
        out.put("rows", rows);
        out.put("note", "Oracle: allocated extents of " + ORACLE_TABLE + " and its indexes. InfluxDB: whole bucket '"
                + influx.getBucket() + "' (the benchmark measurement plus the 120-point sensor_reading sample).");
        out.put("timestamp", Instant.now().toString());
        return out;
    }

    private static Map<String, Object> stripDataset(Map<String, Object> seed) {
        Map<String, Object> m = new LinkedHashMap<>(seed);
        m.remove("dataset");
        return m;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Query specs (SQL + Flux for the same question)
    // ═════════════════════════════════════════════════════════════════════

    /** Normalised result: key -> metric -> value. */
    private static final class Norm extends TreeMap<String, Map<String, Double>> {
        private static final long serialVersionUID = 1L;
    }

    private record Spec(String sql, Object[] params, String flux,
                        Function<List<Map<String, Object>>, Norm> oracleNorm,
                        Function<List<Map<String, String>>, Norm> influxNorm,
                        String chartKind, String primaryMetric, String chartTitle, String unit) {}

    private record Window(LocalDateTime start, LocalDateTime stop) {}

    private String fluxBase(Window w, String extraFilter) {
        return "from(bucket: \"" + influx.getBucket() + "\")\n"
                + "  |> range(start: " + ISO.format(w.start()) + ", stop: " + ISO.format(w.stop()) + ")\n"
                + "  |> filter(fn: (r) => r._measurement == \"" + MEASUREMENT + "\" and r._field == \"value\""
                + (extraFilter == null ? "" : " and " + extraFilter) + ")";
    }

    private Spec buildSpec(QueryDef def, Window w, long deviceId, double threshold) {
        String T = ORACLE_TABLE;
        Object start = Timestamp.valueOf(w.start());
        Object stop = Timestamp.valueOf(w.stop());
        String dev = "r.device_id == \"" + deviceId + "\"";
        String thr = String.format(Locale.ROOT, "%.2f", threshold);

        switch (def.key()) {
            case "RAW_RANGE" -> {
                return new Spec(
                        "SELECT reading_time, value FROM " + T
                                + " WHERE device_id = ? AND reading_time >= ? AND reading_time < ? ORDER BY reading_time",
                        new Object[]{deviceId, start, stop},
                        fluxBase(w, dev) + "\n  |> keep(columns: [\"_time\", \"_value\"])",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(iso(r.get("reading_time")), metrics("value", r.get("value")));
                            return n;
                        },
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(r.get("_time"), metrics("value", parseD(r.get("_value"))));
                            return n;
                        },
                        "time", "value", "Raw readings — device " + deviceId, "°C / state");
            }
            case "WINDOW_AGGREGATE" -> {
                return new Spec(
                        "SELECT TRUNC(reading_time, 'HH') AS window_start, AVG(value) AS avg_value FROM " + T
                                + " WHERE device_id = ? AND reading_time >= ? AND reading_time < ?"
                                + " GROUP BY TRUNC(reading_time, 'HH') ORDER BY window_start",
                        new Object[]{deviceId, start, stop},
                        fluxBase(w, dev)
                                + "\n  |> aggregateWindow(every: 1h, fn: mean, createEmpty: false, timeSrc: \"_start\")"
                                + "\n  |> keep(columns: [\"_time\", \"_value\"])",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(iso(r.get("window_start")), metrics("avg", r.get("avg_value")));
                            return n;
                        },
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(r.get("_time"), metrics("avg", parseD(r.get("_value"))));
                            return n;
                        },
                        "time", "avg", "Hourly average — device " + deviceId, "avg value");
            }
            case "DEVICE_SUMMARY" -> {
                return new Spec(
                        "SELECT device_id, COUNT(*) AS cnt, AVG(value) AS avg_value, MIN(value) AS min_value, MAX(value) AS max_value FROM " + T
                                + " WHERE reading_time >= ? AND reading_time < ? GROUP BY device_id ORDER BY device_id",
                        new Object[]{start, stop},
                        "data = " + fluxBase(w, null) + "\n  |> group(columns: [\"device_id\"])\n\n"
                                + "data |> count() |> yield(name: \"count\")\n"
                                + "data |> mean() |> yield(name: \"avg\")\n"
                                + "data |> min() |> yield(name: \"min\")\n"
                                + "data |> max() |> yield(name: \"max\")",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) {
                                Map<String, Double> m = new LinkedHashMap<>();
                                m.put("count", num(r.get("cnt")));
                                m.put("avg", num(r.get("avg_value")));
                                m.put("min", num(r.get("min_value")));
                                m.put("max", num(r.get("max_value")));
                                n.put(devKey(r.get("device_id")), m);
                            }
                            return n;
                        },
                        CompareService::normMultiYield,
                        "device", "avg", "Average value per device", "avg value");
            }
            case "LATEST_PER_DEVICE" -> {
                return new Spec(
                        "SELECT device_id, MAX(reading_time) AS last_time,"
                                + " MAX(value) KEEP (DENSE_RANK LAST ORDER BY reading_time) AS last_value FROM " + T
                                + " WHERE reading_time >= ? AND reading_time < ? GROUP BY device_id ORDER BY device_id",
                        new Object[]{start, stop},
                        fluxBase(w, null) + "\n  |> last()\n  |> keep(columns: [\"device_id\", \"_time\", \"_value\"])",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) {
                                Map<String, Double> m = new LinkedHashMap<>();
                                m.put("value", num(r.get("last_value")));
                                m.put("time", epoch(r.get("last_time")));
                                n.put(devKey(r.get("device_id")), m);
                            }
                            return n;
                        },
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) {
                                Map<String, Double> m = new LinkedHashMap<>();
                                m.put("value", parseD(r.get("_value")));
                                m.put("time", (double) Instant.parse(r.get("_time")).getEpochSecond());
                                n.put(r.get("device_id"), m);
                            }
                            return n;
                        },
                        "device", "value", "Latest value per device", "value");
            }
            case "THRESHOLD_ALERTS" -> {
                return new Spec(
                        "SELECT device_id, COUNT(*) AS cnt, MAX(value) AS max_value FROM " + T
                                + " WHERE value > ? AND reading_time >= ? AND reading_time < ? GROUP BY device_id ORDER BY device_id",
                        new Object[]{BigDecimal.valueOf(threshold), start, stop},
                        "breaches = " + fluxBase(w, null)
                                + "\n  |> filter(fn: (r) => r._value > " + thr + ")"
                                + "\n  |> group(columns: [\"device_id\"])\n\n"
                                + "breaches |> count() |> yield(name: \"count\")\n"
                                + "breaches |> max() |> yield(name: \"max\")",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) {
                                Map<String, Double> m = new LinkedHashMap<>();
                                m.put("count", num(r.get("cnt")));
                                m.put("max", num(r.get("max_value")));
                                n.put(devKey(r.get("device_id")), m);
                            }
                            return n;
                        },
                        CompareService::normMultiYield,
                        "device", "count", "Readings above " + thr + " per device", "breaches");
            }
            case "DAILY_DOWNSAMPLE" -> {
                return new Spec(
                        "SELECT device_id, TRUNC(reading_time, 'DD') AS day_start, AVG(value) AS avg_value FROM " + T
                                + " WHERE reading_time >= ? AND reading_time < ?"
                                + " GROUP BY device_id, TRUNC(reading_time, 'DD') ORDER BY device_id, day_start",
                        new Object[]{start, stop},
                        fluxBase(w, null)
                                + "\n  |> aggregateWindow(every: 1d, fn: mean, createEmpty: false, timeSrc: \"_start\")"
                                + "\n  |> keep(columns: [\"device_id\", \"_time\", \"_value\"])",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(devKey(r.get("device_id")) + "|" + iso(r.get("day_start")), metrics("avg", r.get("avg_value")));
                            return n;
                        },
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) n.put(r.get("device_id") + "|" + r.get("_time"), metrics("avg", parseD(r.get("_value"))));
                            return n;
                        },
                        "day", "avg", "Daily average (mean of all devices per day)", "avg value");
            }
            case "GLOBAL_AGGREGATE" -> {
                return new Spec(
                        "SELECT COUNT(*) AS cnt, AVG(value) AS avg_value, MIN(value) AS min_value, MAX(value) AS max_value FROM " + T
                                + " WHERE reading_time >= ? AND reading_time < ?",
                        new Object[]{start, stop},
                        "data = " + fluxBase(w, null) + "\n  |> group()\n\n"
                                + "data |> count() |> yield(name: \"count\")\n"
                                + "data |> mean() |> yield(name: \"avg\")\n"
                                + "data |> min() |> yield(name: \"min\")\n"
                                + "data |> max() |> yield(name: \"max\")",
                        rows -> {
                            Norm n = new Norm();
                            for (var r : rows) {
                                if (num(r.get("cnt")) == null || num(r.get("cnt")) == 0) continue;
                                Map<String, Double> m = new LinkedHashMap<>();
                                m.put("count", num(r.get("cnt")));
                                m.put("avg", num(r.get("avg_value")));
                                m.put("min", num(r.get("min_value")));
                                m.put("max", num(r.get("max_value")));
                                n.put("ALL", m);
                            }
                            return n;
                        },
                        rows -> {
                            Norm n = new Norm();
                            Map<String, Double> m = new LinkedHashMap<>();
                            for (var r : rows) m.put(r.get("result"), parseD(r.get("_value")));
                            if (!m.isEmpty()) n.put("ALL", m);
                            return n;
                        },
                        "single", "avg", "Global statistics", "value");
            }
            default -> throw new IllegalArgumentException("No read spec for " + def.key());
        }
    }

    /** Multi-yield Flux result (count / avg / min / max yields) keyed by device_id. */
    private static Norm normMultiYield(List<Map<String, String>> rows) {
        Norm n = new Norm();
        for (var r : rows) {
            String dev = r.get("device_id");
            String metric = r.get("result");
            if (dev == null || metric == null) continue;
            n.computeIfAbsent(dev, k -> new LinkedHashMap<>()).put(metric, parseD(r.get("_value")));
        }
        return n;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Running a comparison
    // ═════════════════════════════════════════════════════════════════════

    private static final RowMapper<Map<String, Object>> ROW_MAPPER = (rs, rowNum) -> {
        ResultSetMetaData md = rs.getMetaData();
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            String name = md.getColumnLabel(i).toLowerCase(Locale.ROOT);
            int t = md.getColumnType(i);
            Object v;
            if (t == Types.TIMESTAMP || t == Types.DATE || t == Types.TIMESTAMP_WITH_TIMEZONE || t == -101 || t == -102) {
                Timestamp ts = rs.getTimestamp(i);
                v = ts == null ? null : ts.toLocalDateTime();
            } else if (t == Types.NUMERIC || t == Types.DECIMAL || t == Types.INTEGER || t == Types.BIGINT
                    || t == Types.DOUBLE || t == Types.FLOAT || t == Types.REAL || t == 100 || t == 101) {
                BigDecimal b = rs.getBigDecimal(i);
                v = b == null ? null : b.doubleValue();
            } else {
                v = rs.getString(i);
            }
            m.put(name, v);
        }
        return m;
    };

    private static final class Side {
        final List<Double> samples = new ArrayList<>();
        String error;
        int rowCount;
        Norm norm = new Norm();
    }

    public Map<String, Object> run(String queryType, Integer iterationsIn, Long deviceIdIn, Integer rangeHoursIn,
                                   Double thresholdIn, Integer writeRowsIn) {
        QueryDef def = resolve(queryType);
        int iterations = clamp(iterationsIn == null ? 5 : iterationsIn, 1, 50);
        acquire();
        try {
            Map<String, Object> result = def.write()
                    ? runWrite(def, iterations, writeRowsIn == null ? 5000 : writeRowsIn)
                    : runRead(def, iterations, deviceIdIn, rangeHoursIn, thresholdIn == null ? 28.0 : thresholdIn);
            remember(result);
            return result;
        } finally {
            lock.unlock();
        }
    }

    private Map<String, Object> runRead(QueryDef def, int iterations, Long deviceIdIn, Integer rangeHoursIn, double threshold) {
        Bounds b;
        try {
            b = oracleBounds();
        } catch (Exception e) {
            throw new IllegalStateException("Oracle is not reachable: " + rootMessage(e));
        }
        if (b.count() == 0 || b.min() == null) {
            throw new IllegalStateException("The benchmark dataset is empty. Generate it first (section 01 · Dataset, or POST /api/compare/seed).");
        }

        // Time window [start, stop) aligned to whole hours (or whole days for daily windows) so that
        // Oracle's TRUNC() buckets and InfluxDB's aggregateWindow() buckets line up exactly.
        int rangeHours = rangeHoursIn == null ? def.defaultRangeHours() : rangeHoursIn;
        LocalDateTime stop;
        LocalDateTime start;
        if ("DAY".equals(def.align())) {
            stop = b.max().truncatedTo(ChronoUnit.DAYS).plusDays(1);
            start = rangeHours <= 0 ? b.min().truncatedTo(ChronoUnit.DAYS)
                    : stop.minusHours(rangeHours).truncatedTo(ChronoUnit.DAYS);
        } else {
            stop = b.max().truncatedTo(ChronoUnit.HOURS).plusHours(1);
            start = rangeHours <= 0 ? b.min().truncatedTo(ChronoUnit.HOURS) : stop.minusHours(rangeHours);
            LocalDateTime minAligned = b.min().truncatedTo(ChronoUnit.HOURS);
            if (start.isBefore(minAligned)) start = minAligned;
        }
        Window w = new Window(start, stop);

        List<Device> devices = loadSensorDevices();
        Map<String, String> names = new HashMap<>();
        for (Device d : devices) names.put(String.valueOf(d.id()), d.name());
        long deviceId = deviceIdIn != null ? deviceIdIn
                : devices.stream().filter(d -> "TEMPERATURE_SENSOR".equals(d.subtype())).map(Device::id).findFirst()
                .orElse(devices.isEmpty() ? 4L : devices.get(0).id());

        Spec spec = buildSpec(def, w, deviceId, threshold);
        Side oracle = new Side();
        Side inf = new Side();

        // Untimed warm-up: parses/plans the SQL, warms caches & connections on both sides.
        execOracle(spec, oracle, false);
        execInflux(spec, inf, false);
        for (int i = 0; i < iterations; i++) {
            if (i % 2 == 0) {
                execOracle(spec, oracle, true);
                execInflux(spec, inf, true);
            } else {
                execInflux(spec, inf, true);
                execOracle(spec, oracle, true);
            }
        }

        Map<String, Object> res = baseResult(def, iterations, b.count(), oracle, inf);
        Map<String, Object> params = new LinkedHashMap<>();
        boolean oneDevice = "One device".equals(def.scope());
        params.put("deviceId", oneDevice ? deviceId : null);
        params.put("deviceName", oneDevice ? names.get(String.valueOf(deviceId)) : "All sensor devices");
        params.put("start", ISO.format(w.start()));
        params.put("stop", ISO.format(w.stop()));
        params.put("rangeHours", ChronoUnit.HOURS.between(w.start(), w.stop()));
        params.put("threshold", "THRESHOLD_ALERTS".equals(def.key()) ? threshold : null);
        res.put("params", params);
        res.put("oracleQuery", inlineSql(spec.sql(), spec.params()));
        res.put("fluxQuery", spec.flux());

        Map<String, Object> cmp = compare(oracle.norm, inf.norm, oracle.error == null && inf.error == null);
        res.put("resultComparison", cmp);
        res.put("chart", buildChart(spec, oracle.norm, inf.norm, names));
        res.put("summary", summarise(def, res, params, cmp));
        return res;
    }

    private void execOracle(Spec spec, Side side, boolean timed) {
        if (side.error != null) return;
        try {
            long t0 = System.nanoTime();
            List<Map<String, Object>> rows = jdbc.query(spec.sql(), ROW_MAPPER, spec.params());
            double ms = msSince(t0);
            if (timed) side.samples.add(ms);
            side.rowCount = rows.size();
            side.norm = spec.oracleNorm().apply(rows);
        } catch (Exception e) {
            side.error = "Oracle query failed: " + rootMessage(e);
            log.warn(side.error);
        }
    }

    private void execInflux(Spec spec, Side side, boolean timed) {
        if (side.error != null) return;
        try {
            long t0 = System.nanoTime();
            List<Map<String, String>> rows = FluxCsvParser.parse(influx.queryCsvStrict(spec.flux()));
            double ms = msSince(t0);
            if (timed) side.samples.add(ms);
            side.rowCount = rows.size();
            side.norm = spec.influxNorm().apply(rows);
        } catch (Exception e) {
            side.error = rootMessage(e);
            log.warn("InfluxDB benchmark query failed: {}", side.error);
        }
    }

    // ── Write benchmark ────────────────────────────────────────────────

    private Map<String, Object> runWrite(QueryDef def, int iterations, int writeRowsIn) {
        int writeRows = clamp(writeRowsIn, 100, 100_000);
        iterations = Math.min(iterations, 10);
        requireBothUp();
        ensureSchema();
        List<Device> devices = loadSensorDevices();
        if (devices.isEmpty()) throw new IllegalStateException("No sensor devices found in the DEVICES table.");

        int perDevice = (int) Math.ceil((double) writeRows / devices.size());
        LocalDateTime base = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS).minusDays(2);   // inside any bucket retention period
        List<Reading> rows = generate(devices, base, base.plusMinutes(perDevice), 1, 99);
        if (rows.size() > writeRows) rows = new ArrayList<>(rows.subList(0, writeRows));
        final List<Reading> batch = rows;
        LocalDateTime last = batch.stream().map(Reading::time).max(Comparator.naturalOrder()).orElse(base);

        Side oracle = new Side();
        Side inf = new Side();
        for (int i = 0; i <= iterations; i++) {             // i == 0 is the untimed warm-up
            boolean timed = i > 0;
            clearWriteTargets();
            Runnable o = () -> {
                if (oracle.error != null) return;
                try {
                    long t0 = System.nanoTime();
                    oracleInsert(ORACLE_WRITE_TABLE, batch);
                    if (timed) oracle.samples.add(msSince(t0));
                } catch (Exception e) {
                    oracle.error = "Oracle insert failed: " + rootMessage(e);
                }
            };
            Runnable f = () -> {
                if (inf.error != null) return;
                try {
                    long t0 = System.nanoTime();
                    influxWrite(WRITE_MEASUREMENT, batch);
                    if (timed) inf.samples.add(msSince(t0));
                } catch (Exception e) {
                    inf.error = rootMessage(e);
                }
            };
            if (i % 2 == 0) { o.run(); f.run(); } else { f.run(); o.run(); }
        }

        // Verify what landed in each database (untimed): row count + checksum of values
        String fStart = ISO.format(base);
        String fStop = ISO.format(last.plusMinutes(1));
        if (oracle.error == null) {
            try {
                Map<String, Object> r = jdbc.queryForObject("SELECT COUNT(*) AS cnt, SUM(value) AS total FROM " + ORACLE_WRITE_TABLE, ROW_MAPPER);
                oracle.rowCount = batch.size();
                Map<String, Double> m = new LinkedHashMap<>();
                m.put("count", num(r.get("cnt")));
                m.put("sum", num(r.get("total")));
                oracle.norm.put("WRITTEN", m);
            } catch (Exception e) {
                oracle.error = "Oracle verification failed: " + rootMessage(e);
            }
        }
        if (inf.error == null) {
            try {
                String flux = "data = from(bucket: \"" + influx.getBucket() + "\")\n"
                        + "  |> range(start: " + fStart + ", stop: " + fStop + ")\n"
                        + "  |> filter(fn: (r) => r._measurement == \"" + WRITE_MEASUREMENT + "\" and r._field == \"value\")\n"
                        + "  |> group()\n"
                        + "data |> count() |> yield(name: \"count\")\n"
                        + "data |> sum() |> yield(name: \"sum\")";
                Map<String, Double> m = new LinkedHashMap<>();
                for (var r : FluxCsvParser.parse(influx.queryCsvStrict(flux))) m.put(r.get("result"), parseD(r.get("_value")));
                inf.rowCount = batch.size();
                if (!m.isEmpty()) inf.norm.put("WRITTEN", m);
            } catch (Exception e) {
                inf.error = "InfluxDB verification failed: " + rootMessage(e);
            }
        }
        try {
            clearWriteTargets();
        } catch (Exception e) {
            log.warn("Cleanup of write benchmark failed: {}", rootMessage(e));
        }

        long datasetSize = 0;
        try { datasetSize = oracleBounds().count(); } catch (Exception ignored) { }
        Map<String, Object> res = baseResult(def, iterations, datasetSize, oracle, inf);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("rowsPerBatch", batch.size());
        params.put("devices", devices.size());
        params.put("oracleBatchSize", ORACLE_BATCH);
        params.put("influxBatchSize", INFLUX_BATCH);
        res.put("params", params);
        res.put("oracleQuery", "INSERT INTO " + ORACLE_WRITE_TABLE + " (device_id, reading_time, value) VALUES (?, ?, ?)\n"
                + "-- executed as JDBC batch: " + batch.size() + " rows, " + ORACLE_BATCH + " rows per executeBatch(), autocommit\n"
                + "-- Oracle maintains PRIMARY KEY (device_id, reading_time), index on reading_time and FOREIGN KEY -> DEVICES");
        Reading s = batch.get(0);
        res.put("fluxQuery", "POST /api/v2/write?bucket=" + influx.getBucket() + "&precision=s\n\n"
                + WRITE_MEASUREMENT + ",device_id=" + s.deviceId() + ",device_type=" + s.subtype() + " value=" + s.value().toPlainString()
                + " " + s.time().toEpochSecond(ZoneOffset.UTC) + "\n... (" + batch.size() + " lines of Line Protocol, "
                + INFLUX_BATCH + " lines per HTTP request)");
        Map<String, Object> cmp = compare(oracle.norm, inf.norm, oracle.error == null && inf.error == null);
        res.put("resultComparison", cmp);

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("kind", "throughput");
        chart.put("title", "Ingestion throughput (rows per second, higher is better)");
        chart.put("unit", "rows/s");
        List<Map<String, Object>> pts = new ArrayList<>();
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("label", batch.size() + " rows");
        p.put("oracle", rowsPerSec(batch.size(), oracle));
        p.put("influx", rowsPerSec(batch.size(), inf));
        pts.add(p);
        chart.put("points", pts);
        res.put("chart", chart);
        res.put("oracleRowsPerSec", rowsPerSec(batch.size(), oracle));
        res.put("influxRowsPerSec", rowsPerSec(batch.size(), inf));
        res.put("summary", summarise(def, res, params, cmp));
        return res;
    }

    private void clearWriteTargets() {
        jdbc.execute("TRUNCATE TABLE " + ORACLE_WRITE_TABLE);
        influx.deleteMeasurementStrict(WRITE_MEASUREMENT);
    }

    private static Long rowsPerSec(int rows, Side s) {
        if (s.error != null || s.samples.isEmpty()) return null;
        double avg = s.samples.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Math.round(rows / Math.max(avg, 0.001) * 1000);
    }

    // ── Run-all suite ─────────────────────────────────────────────────

    public Map<String, Object> runAll(Integer iterationsIn, Integer writeRowsIn) {
        int iterations = clamp(iterationsIn == null ? 3 : iterationsIn, 1, 20);
        List<Map<String, Object>> results = new ArrayList<>();
        int oracleWins = 0, influxWins = 0, ties = 0, matches = 0;
        double oracleTotal = 0, influxTotal = 0;
        for (QueryDef def : CATALOG.values()) {
            Map<String, Object> entry;
            try {
                Map<String, Object> r = run(def.key(), iterations, null, null, null, writeRowsIn);
                entry = new LinkedHashMap<>(summaryOf(r));
                entry.put("oracle", r.get("oracle"));
                entry.put("influx", r.get("influx"));
                entry.put("summary", r.get("summary"));
                entry.put("oracleQuery", r.get("oracleQuery"));
                entry.put("fluxQuery", r.get("fluxQuery"));
                entry.put("oracleRowsPerSec", r.get("oracleRowsPerSec"));
                entry.put("influxRowsPerSec", r.get("influxRowsPerSec"));
                Object o = r.get("oracleMs");
                Object f = r.get("influxMs");
                if (o instanceof Number on && f instanceof Number fn) {
                    oracleTotal += on.doubleValue();
                    influxTotal += fn.doubleValue();
                }
                switch (String.valueOf(r.get("winner"))) {
                    case "ORACLE" -> oracleWins++;
                    case "INFLUX" -> influxWins++;
                    case "TIE" -> ties++;
                    default -> { }
                }
                if ("MATCH".equals(entry.get("matchStatus"))) matches++;
            } catch (Exception e) {
                entry = new LinkedHashMap<>();
                entry.put("queryType", def.key());
                entry.put("label", def.label());
                entry.put("error", rootMessage(e));
            }
            results.add(entry);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("iterations", iterations);
        out.put("results", results);
        out.put("oracleWins", oracleWins);
        out.put("influxWins", influxWins);
        out.put("ties", ties);
        out.put("resultsMatched", matches);
        out.put("queries", results.size());
        out.put("oracleTotalMs", round(oracleTotal, 2));
        out.put("influxTotalMs", round(influxTotal, 2));
        String overall = influxWins > oracleWins ? "INFLUX" : oracleWins > influxWins ? "ORACLE" : "TIE";
        out.put("overallWinner", overall);
        out.put("analysis", String.format(Locale.ROOT,
                "Across %d workloads InfluxDB was faster in %d and Oracle in %d%s. Summed average latency: Oracle %.2f ms vs InfluxDB %.2f ms. "
                        + "%d of %d workloads returned identical results in both databases.",
                results.size(), influxWins, oracleWins, ties > 0 ? " (" + ties + " tie" + (ties > 1 ? "s" : "") + ")" : "",
                oracleTotal, influxTotal, matches, results.size()));
        out.put("timestamp", Instant.now().toString());
        return out;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Result assembly helpers
    // ═════════════════════════════════════════════════════════════════════

    private Map<String, Object> baseResult(QueryDef def, int iterations, long datasetSize, Side oracle, Side inf) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("queryType", def.key());
        res.put("label", def.label());
        res.put("category", def.category());
        res.put("scope", def.scope());
        res.put("description", def.description());
        res.put("explanation", def.explanation());
        res.put("iterations", iterations);
        res.put("datasetSize", datasetSize);

        Map<String, Object> os = stats(oracle);
        Map<String, Object> is = stats(inf);
        res.put("oracle", os);
        res.put("influx", is);
        Double o = (Double) os.get("avgMs");
        Double f = (Double) is.get("avgMs");
        res.put("oracleMs", o);
        res.put("influxMs", f);
        res.put("oracleRowCount", oracle.error == null ? oracle.rowCount : null);
        res.put("influxRowCount", inf.error == null ? inf.rowCount : null);
        res.put("oracleError", oracle.error);
        res.put("influxError", inf.error);
        if (o != null && f != null) {
            double faster = Math.min(o, f);
            double slower = Math.max(o, f);
            String winner = Math.abs(o - f) < 0.0005 ? "TIE" : (o < f ? "ORACLE" : "INFLUX");
            res.put("winner", winner);
            res.put("speedupFactor", round(slower / Math.max(faster, 0.001), 2));
            res.put("timeDifferenceMs", round(slower - faster, 3));
            res.put("percentFaster", round((slower - faster) / Math.max(slower, 0.001) * 100, 1));
        } else {
            res.put("winner", null);
            res.put("speedupFactor", null);
            res.put("timeDifferenceMs", null);
            res.put("percentFaster", null);
        }
        res.put("isFallback", false);
        res.put("timestamp", Instant.now().toString());
        return res;
    }

    private static Map<String, Object> stats(Side s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", s.error);
        if (s.samples.isEmpty()) {
            m.put("avgMs", null);
            m.put("samplesMs", List.of());
            return m;
        }
        List<Double> sorted = new ArrayList<>(s.samples);
        Collections.sort(sorted);
        double avg = sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double var = sorted.stream().mapToDouble(x -> (x - avg) * (x - avg)).sum() / sorted.size();
        double median = sorted.size() % 2 == 1 ? sorted.get(sorted.size() / 2)
                : (sorted.get(sorted.size() / 2 - 1) + sorted.get(sorted.size() / 2)) / 2;
        int p95i = (int) Math.ceil(0.95 * sorted.size()) - 1;
        m.put("avgMs", round(avg, 3));
        m.put("minMs", round(sorted.get(0), 3));
        m.put("maxMs", round(sorted.get(sorted.size() - 1), 3));
        m.put("medianMs", round(median, 3));
        m.put("p95Ms", round(sorted.get(Math.max(0, p95i)), 3));
        m.put("stdDevMs", round(Math.sqrt(var), 3));
        m.put("samplesMs", s.samples.stream().map(x -> round(x, 3)).toList());
        m.put("rowCount", s.error == null ? s.rowCount : null);
        return m;
    }

    private static Map<String, Object> compare(Norm o, Norm f, boolean bothOk) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!bothOk) {
            out.put("status", "UNAVAILABLE");
            out.put("message", "Results could not be compared because one of the databases returned an error.");
            out.put("sample", List.of());
            return out;
        }
        TreeSet<String> keys = new TreeSet<>(o.keySet());
        keys.addAll(f.keySet());
        int matched = 0, mismatched = 0, oracleOnly = 0, influxOnly = 0;
        double maxDiff = 0;
        List<Map<String, Object>> sample = new ArrayList<>();
        List<Map<String, Object>> mismatchSample = new ArrayList<>();
        for (String k : keys) {
            Map<String, Double> a = o.get(k);
            Map<String, Double> b = f.get(k);
            boolean ok;
            if (a == null) { influxOnly++; ok = false; }
            else if (b == null) { oracleOnly++; ok = false; }
            else {
                ok = true;
                for (var e : a.entrySet()) {
                    Double x = e.getValue();
                    Double y = b.get(e.getKey());
                    if (x == null && y == null) continue;
                    if (x == null || y == null) { ok = false; continue; }
                    double d = Math.abs(x - y);
                    maxDiff = Math.max(maxDiff, d);
                    double tol = Math.max(0.001, Math.abs(x) * 1e-9);
                    if (d > tol) ok = false;
                }
                if (ok) matched++; else mismatched++;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", k);
            row.put("oracle", a);
            row.put("influx", b);
            row.put("match", ok);
            if (sample.size() < 12) sample.add(row);
            else if (!ok && mismatchSample.size() < 5) mismatchSample.add(row);
        }
        sample.addAll(mismatchSample);
        String status;
        if (mismatched == 0 && oracleOnly == 0 && influxOnly == 0) status = "MATCH";
        else if (matched > 0) status = "PARTIAL";
        else status = "MISMATCH";
        out.put("status", status);
        out.put("totalKeys", keys.size());
        out.put("matched", matched);
        out.put("mismatched", mismatched);
        out.put("oracleOnly", oracleOnly);
        out.put("influxOnly", influxOnly);
        out.put("maxAbsDifference", round(maxDiff, 6));
        out.put("message", switch (status) {
            case "MATCH" -> keys.isEmpty() ? "Both databases returned an empty result."
                    : "Both databases returned identical answers for all " + keys.size() + " result rows.";
            case "PARTIAL" -> matched + " of " + keys.size() + " result rows match; " + (mismatched + oracleOnly + influxOnly) + " differ.";
            default -> "The databases returned different answers.";
        });
        out.put("sample", sample);
        return out;
    }

    private static Map<String, Object> buildChart(Spec spec, Norm o, Norm f, Map<String, String> names) {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("kind", spec.chartKind());
        chart.put("title", spec.chartTitle());
        chart.put("unit", spec.unit());
        chart.put("metric", spec.primaryMetric());
        List<Map<String, Object>> pts = new ArrayList<>();
        String metric = spec.primaryMetric();
        switch (spec.chartKind()) {
            case "time" -> {
                TreeSet<String> keys = new TreeSet<>(o.keySet());
                keys.addAll(f.keySet());
                List<String> list = new ArrayList<>(keys);
                int stride = Math.max(1, (int) Math.ceil(list.size() / 300.0));
                for (int i = 0; i < list.size(); i += stride) {
                    String k = list.get(i);
                    pts.add(point(k, val(o, k, metric), val(f, k, metric)));
                }
            }
            case "device" -> {
                TreeSet<String> keys = new TreeSet<>(Comparator.comparingLong(CompareService::safeLong));
                keys.addAll(o.keySet());
                keys.addAll(f.keySet());
                for (String k : keys) {
                    Map<String, Object> p = point(names.getOrDefault(k, "Device " + k), val(o, k, metric), val(f, k, metric));
                    p.put("deviceId", k);
                    pts.add(p);
                }
            }
            case "day" -> {
                TreeMap<String, double[]> agg = new TreeMap<>();   // day -> [oSum, oN, fSum, fN]
                for (var e : o.entrySet()) {
                    String day = e.getKey().substring(e.getKey().indexOf('|') + 1);
                    Double v = e.getValue().get(metric);
                    if (v != null) { double[] a = agg.computeIfAbsent(day, x -> new double[4]); a[0] += v; a[1]++; }
                }
                for (var e : f.entrySet()) {
                    String day = e.getKey().substring(e.getKey().indexOf('|') + 1);
                    Double v = e.getValue().get(metric);
                    if (v != null) { double[] a = agg.computeIfAbsent(day, x -> new double[4]); a[2] += v; a[3]++; }
                }
                for (var e : agg.entrySet()) {
                    double[] a = e.getValue();
                    pts.add(point(e.getKey(), a[1] > 0 ? round(a[0] / a[1], 4) : null, a[3] > 0 ? round(a[2] / a[3], 4) : null));
                }
            }
            default -> {
                for (String m : List.of("avg", "min", "max")) {
                    pts.add(point(m.toUpperCase(Locale.ROOT), val(o, "ALL", m), val(f, "ALL", m)));
                }
            }
        }
        chart.put("points", pts);
        return chart;
    }

    private static Map<String, Object> point(String label, Double o, Double f) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("label", label);
        p.put("oracle", o);
        p.put("influx", f);
        return p;
    }

    private static Double val(Norm n, String key, String metric) {
        Map<String, Double> m = n.get(key);
        if (m == null) return null;
        Double v = m.get(metric);
        return v == null ? null : round(v, 4);
    }

    private static String summarise(QueryDef def, Map<String, Object> res, Map<String, Object> params, Map<String, Object> cmp) {
        Object o = res.get("oracleMs");
        Object f = res.get("influxMs");
        if (!(o instanceof Double om) || !(f instanceof Double fm)) {
            List<String> errs = new ArrayList<>();
            if (res.get("oracleError") != null) errs.add(String.valueOf(res.get("oracleError")));
            if (res.get("influxError") != null) errs.add(String.valueOf(res.get("influxError")));
            return "The comparison could not be completed: " + String.join(" | ", errs);
        }
        String winner = String.valueOf(res.get("winner"));
        String who = "INFLUX".equals(winner) ? "InfluxDB" : "ORACLE".equals(winner) ? "Oracle" : "Neither database";
        String scope = params.get("deviceName") != null ? " for " + params.get("deviceName") : "";
        String rows = def.write() ? params.get("rowsPerBatch") + " rows written per run"
                : "Oracle returned " + res.get("oracleRowCount") + " rows, InfluxDB " + res.get("influxRowCount") + " rows";
        String verdict = "TIE".equals(winner)
                ? "Both took the same time on average."
                : String.format(Locale.ROOT, "%s was %.2f× faster, saving %.2f ms per %s (%.1f%% less time).",
                who, (Double) res.get("speedupFactor"), (Double) res.get("timeDifferenceMs"), def.write() ? "batch" : "query",
                (Double) res.get("percentFaster"));
        return String.format(Locale.ROOT, "%s%s: Oracle averaged %.2f ms and InfluxDB %.2f ms over %d timed runs. %s %s. %s",
                def.label(), scope, om, fm, (Integer) res.get("iterations"), verdict, rows, cmp.get("message"));
    }

    private void remember(Map<String, Object> result) {
        Map<String, Object> s = summaryOf(result);
        s.put("id", System.nanoTime());
        history.addFirst(s);
        while (history.size() > 100) history.pollLast();
    }

    private static Map<String, Object> summaryOf(Map<String, Object> r) {
        Map<String, Object> s = new LinkedHashMap<>();
        for (String k : List.of("queryType", "label", "category", "oracleMs", "influxMs", "winner", "speedupFactor",
                "timeDifferenceMs", "percentFaster", "oracleRowCount", "influxRowCount", "iterations", "datasetSize",
                "oracleError", "influxError", "timestamp")) {
            s.put(k, r.get(k));
        }
        Object cmp = r.get("resultComparison");
        s.put("matchStatus", cmp instanceof Map<?, ?> m ? m.get("status") : null);
        return s;
    }

    public List<Map<String, Object>> history() {
        return new ArrayList<>(history);
    }

    public void clearHistory() {
        history.clear();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Small utilities
    // ═════════════════════════════════════════════════════════════════════

    private void acquire() {
        try {
            if (!lock.tryLock(5, TimeUnit.MINUTES)) {
                throw new IllegalStateException("Another benchmark is still running. Please wait for it to finish.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for another benchmark to finish.");
        }
    }

    private static String inlineSql(String sql, Object[] params) {
        StringBuilder sb = new StringBuilder();
        int p = 0;
        for (char c : sql.toCharArray()) {
            if (c == '?' && p < params.length) {
                Object v = params[p++];
                if (v instanceof Timestamp ts) sb.append("TIMESTAMP '").append(SQL_TS.format(ts.toLocalDateTime())).append('\'');
                else if (v instanceof String s) sb.append('\'').append(s.replace("'", "''")).append('\'');
                else sb.append(v);
            } else {
                sb.append(c);
            }
        }
        return sb.toString()
                .replace(" FROM ", "\nFROM ")
                .replace(" WHERE ", "\nWHERE ")
                .replace(" GROUP BY ", "\nGROUP BY ")
                .replace(" ORDER BY ", "\nORDER BY ");
    }

    private static Map<String, Double> metrics(String name, Object v) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(name, num(v));
        return m;
    }

    private static Double num(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        return parseD(String.valueOf(v));
    }

    private static Double parseD(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String devKey(Object v) {
        if (v instanceof Number n) return String.valueOf(n.longValue());
        return String.valueOf(v);
    }

    private static long safeLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }

    private static String iso(Object v) {
        if (v instanceof LocalDateTime t) return ISO.format(t);
        if (v instanceof Timestamp ts) return ISO.format(ts.toLocalDateTime());
        return String.valueOf(v);
    }

    private static Double epoch(Object v) {
        if (v instanceof LocalDateTime t) return (double) t.toEpochSecond(ZoneOffset.UTC);
        if (v instanceof Timestamp ts) return (double) ts.toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
        return null;
    }

    private static double msSince(long t0) {
        return (System.nanoTime() - t0) / 1_000_000.0;
    }

    private static Double round(double v, int places) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return BigDecimal.valueOf(v).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        String m = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        if (e != t && e.getMessage() != null && e instanceof IllegalStateException) m = e.getMessage();
        return m.replaceAll("\\s+", " ").trim();
    }
}
