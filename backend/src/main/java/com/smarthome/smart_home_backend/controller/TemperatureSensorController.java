package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.TemperatureSensor;
import com.smarthome.smart_home_backend.service.TemperatureSensorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/temperature-sensors")
public class TemperatureSensorController {

    private final TemperatureSensorService sensorService;

    public TemperatureSensorController(
            TemperatureSensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public List<TemperatureSensor> getAllSensors() {
        return sensorService.getAllSensors();
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<TemperatureSensor> getSensorById(
            @PathVariable Long deviceId) {

        return sensorService.getSensorById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<TemperatureSensor> createSensor(
            @PathVariable Long deviceId,
            @RequestBody TemperatureSensor sensor) {

        try {
            return ResponseEntity.ok(
                    sensorService.createSensor(deviceId, sensor)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<TemperatureSensor> updateSensor(
            @PathVariable Long deviceId,
            @RequestBody TemperatureSensor details) {

        try {
            return ResponseEntity.ok(
                    sensorService.updateSensor(deviceId, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteSensor(
            @PathVariable Long deviceId) {

        try {
            sensorService.deleteSensor(deviceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}