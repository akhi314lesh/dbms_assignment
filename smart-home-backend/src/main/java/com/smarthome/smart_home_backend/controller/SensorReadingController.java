package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.service.SensorReadingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readings")
public class SensorReadingController {

    private final SensorReadingService readingService;

    public SensorReadingController(
            SensorReadingService readingService) {

        this.readingService = readingService;
    }

    @GetMapping
    public List<SensorReading> getAllReadings() {
        return readingService.getAllReadings();
    }

    @GetMapping("/device/{deviceId}")
    public List<SensorReading> getReadingsByDevice(
            @PathVariable Long deviceId) {

        return readingService.getReadingsByDeviceId(deviceId);
    }

    @GetMapping("/{deviceId}/{readingId}")
    public ResponseEntity<SensorReading> getReading(
            @PathVariable Long deviceId,
            @PathVariable Long readingId) {

        return readingService
                .getReading(deviceId, readingId)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<SensorReading> createReading(
            @PathVariable Long deviceId,
            @RequestBody SensorReading reading) {

        try {
            return ResponseEntity.ok(
                    readingService.createReading(
                            deviceId,
                            reading
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}/{readingId}")
    public ResponseEntity<SensorReading> updateReading(
            @PathVariable Long deviceId,
            @PathVariable Long readingId,
            @RequestBody SensorReading details) {

        try {
            return ResponseEntity.ok(
                    readingService.updateReading(
                            deviceId,
                            readingId,
                            details
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}/{readingId}")
    public ResponseEntity<Void> deleteReading(
            @PathVariable Long deviceId,
            @PathVariable Long readingId) {

        try {
            readingService.deleteReading(
                    deviceId,
                    readingId
            );

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}