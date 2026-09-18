package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Thermostat;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.ThermostatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thermostats")
public class ThermostatController {

    private final ThermostatService thermostatService;
    private final HomeAuthorizationService authService;

    public ThermostatController(ThermostatService thermostatService, HomeAuthorizationService authService) {
        this.thermostatService = thermostatService;
        this.authService = authService;
    }

    @GetMapping
    public List<Thermostat> getAllThermostats() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return thermostatService.getAllThermostats();
        }
        return thermostatService.getThermostatsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<Thermostat> getThermostatById(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return thermostatService.getThermostatById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Thermostat> createThermostat(
            @PathVariable Long deviceId,
            @RequestBody Thermostat thermostat) {

        authService.assertCanManageDevice(deviceId);
        try {
            Thermostat savedThermostat = thermostatService.createThermostat(deviceId, thermostat);
            return ResponseEntity.ok(savedThermostat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<Thermostat> updateThermostat(
            @PathVariable Long deviceId,
            @RequestBody Thermostat thermostatDetails) {

        authService.assertCanAccessDevice(deviceId);
        try {
            Thermostat updatedThermostat = thermostatService.updateThermostat(deviceId, thermostatDetails);
            return ResponseEntity.ok(updatedThermostat);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteThermostat(@PathVariable Long deviceId) {
        authService.assertCanManageDevice(deviceId);
        try {
            thermostatService.deleteThermostat(deviceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}