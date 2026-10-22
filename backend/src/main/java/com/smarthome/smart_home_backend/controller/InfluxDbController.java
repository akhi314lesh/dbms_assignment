package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.service.InfluxDbService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/influx")
public class InfluxDbController {

    private final InfluxDbService influxDbService;

    public InfluxDbController(InfluxDbService influxDbService) {
        this.influxDbService = influxDbService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(influxDbService.checkHealth());
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> executeFlux(@RequestBody Map<String, String> body) {
        String flux = body.get("flux");
        if (flux == null || flux.isBlank()) {
            flux = body.get("query");
        }
        return ResponseEntity.ok(influxDbService.executeFlux(flux));
    }

    @GetMapping("/telemetry")
    public ResponseEntity<Map<String, Object>> getTelemetry(
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false, defaultValue = "-30d") String timeRange) {
        return ResponseEntity.ok(influxDbService.getTelemetryPoints(deviceId, timeRange));
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> runBenchmark(@RequestBody(required = false) Map<String, Object> body) {
        Long deviceId = 4L;
        if (body != null && body.containsKey("deviceId")) {
            deviceId = ((Number) body.get("deviceId")).longValue();
        }
        return ResponseEntity.ok(influxDbService.runBenchmark(deviceId));
    }

    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> writeLineProtocol(@RequestBody String lineProtocol) {
        return ResponseEntity.ok(influxDbService.writeLineProtocol(lineProtocol));
    }

    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrateFromOracle() {
        return ResponseEntity.ok(influxDbService.migrateSampleReadings());
    }
}
