package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.service.CompareService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CompareController — DA3 Oracle vs InfluxDB comparison API (T1).
 *
 *   GET    /api/compare/health    → { oracle, influx, oracleVersion, influxVersion, oracleError?, influxError? }
 *   GET    /api/compare/queries   → catalogue of comparable workloads (label, description, explanation)
 *   GET    /api/compare/devices   → sensor devices that are part of the benchmark dataset
 *   GET    /api/compare/dataset   → row counts in Oracle and InfluxDB + whether they are in sync
 *   POST   /api/compare/seed      → { days, intervalMinutes } generate the SAME dataset in both databases
 *   GET    /api/compare/storage   → ?settle=true  bytes used by the dataset in each database
 *   POST   /api/compare/run       → { queryType, iterations, deviceId?, rangeHours?, threshold?, writeRows? }
 *   POST   /api/compare/run-all   → { iterations, writeRows? } run every workload
 *   GET    /api/compare/history   → recent runs (in memory)
 *   DELETE /api/compare/history
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return compareService.health();
    }

    @GetMapping("/queries")
    public List<Map<String, Object>> queries() {
        return compareService.queryCatalog();
    }

    @GetMapping("/devices")
    public List<Map<String, Object>> devices() {
        return compareService.devices();
    }

    @GetMapping("/dataset")
    public Map<String, Object> dataset() {
        return compareService.datasetInfo();
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        return compareService.seed(intOrNull(b.get("days")), intOrNull(b.get("intervalMinutes")));
    }

    @GetMapping("/storage")
    public Map<String, Object> storage(@RequestParam(defaultValue = "false") boolean settle) {
        return compareService.storage(settle);
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        Object qt = b.get("queryType");
        return compareService.run(
                qt == null ? null : String.valueOf(qt),
                intOrNull(b.get("iterations")),
                longOrNull(b.get("deviceId")),
                intOrNull(b.containsKey("rangeHours") ? b.get("rangeHours") : b.get("rangeMinutes") == null ? null
                        : Math.max(1, (int) Math.ceil(toDouble(b.get("rangeMinutes")) / 60.0))),
                doubleOrNull(b.get("threshold")),
                intOrNull(b.get("writeRows")));
    }

    @PostMapping("/run-all")
    public Map<String, Object> runAll(@RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> b = body == null ? Map.of() : body;
        return compareService.runAll(intOrNull(b.get("iterations")), intOrNull(b.get("writeRows")));
    }

    @GetMapping("/history")
    public List<Map<String, Object>> history() {
        return compareService.history();
    }

    @DeleteMapping("/history")
    public Map<String, Object> clearHistory() {
        compareService.clearHistory();
        return Map.of("cleared", true);
    }

    // ── Error handling: always answer with a readable JSON message ──────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> conflict(IllegalStateException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> serverError(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return error(HttpStatus.INTERNAL_SERVER_ERROR, t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", true);
        body.put("status", status.value());
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }

    // ── Lenient body parsing (numbers may arrive as strings from forms) ─────

    private static double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(v).trim());
    }

    private static Integer intOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        try {
            return (int) Math.round(toDouble(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a number but got '" + v + "'");
        }
    }

    private static Long longOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        try {
            return Math.round(toDouble(v));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a number but got '" + v + "'");
        }
    }

    private static Double doubleOrNull(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        try {
            return toDouble(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected a number but got '" + v + "'");
        }
    }
}
