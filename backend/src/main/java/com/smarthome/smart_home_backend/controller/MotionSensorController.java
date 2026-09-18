package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.MotionSensor;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.MotionSensorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/motion-sensors")
public class MotionSensorController {

    private final MotionSensorService sensorService;
    private final HomeAuthorizationService authService;

    public MotionSensorController(MotionSensorService sensorService, HomeAuthorizationService authService) {
        this.sensorService = sensorService;
        this.authService = authService;
    }

    @GetMapping
    public List<MotionSensor> getAllSensors() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return sensorService.getAllSensors();
        }
        return sensorService.getSensorsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<MotionSensor> getSensorById(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return sensorService.getSensorById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<MotionSensor> createSensor(
            @PathVariable Long deviceId,
            @RequestBody MotionSensor sensor) {

        authService.assertCanManageDevice(deviceId);
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

        authService.assertCanAccessDevice(deviceId);
        try {
            return ResponseEntity.ok(
                    sensorService.updateSensor(deviceId, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long deviceId) {
        authService.assertCanManageDevice(deviceId);
        try {
            sensorService.deleteSensor(deviceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}