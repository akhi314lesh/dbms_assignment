package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.MotionSensor;
import com.smarthome.smart_home_backend.service.MotionSensorService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motion-sensors")
public class MotionSensorController {

    private final MotionSensorService sensorService;

    public MotionSensorController(MotionSensorService sensorService) {
        this.sensorService = sensorService;
    }

    @GetMapping
    public List<MotionSensor> getAllSensors() {
        return sensorService.getAllSensors();
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<MotionSensor> getSensorById(
            @PathVariable Long deviceId) {

        return sensorService.getSensorById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<MotionSensor> createSensor(
            @PathVariable Long deviceId,
            @RequestBody MotionSensor sensor) {

        try {
            return ResponseEntity.ok(
                    sensorService.createSensor(deviceId, sensor)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<MotionSensor> updateSensor(
            @PathVariable Long deviceId,
            @RequestBody MotionSensor details) {

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