package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Thermostat;
import com.smarthome.smart_home_backend.service.ThermostatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/thermostats")
public class ThermostatController {

    private final ThermostatService thermostatService;

    public ThermostatController(ThermostatService thermostatService) {
        this.thermostatService = thermostatService;
    }

    // Get all thermostats
    @GetMapping
    public List<Thermostat> getAllThermostats() {
        return thermostatService.getAllThermostats();
    }

    // Get thermostat by device ID
    @GetMapping("/{deviceId}")
    public ResponseEntity<Thermostat> getThermostatById(
            @PathVariable Long deviceId) {

        return thermostatService.getThermostatById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create thermostat for an existing device
    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Thermostat> createThermostat(
            @PathVariable Long deviceId,
            @RequestBody Thermostat thermostat) {

        try {
            Thermostat savedThermostat =
                    thermostatService.createThermostat(
                            deviceId,
                            thermostat
                    );

            return ResponseEntity.ok(savedThermostat);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Update thermostat
    @PutMapping("/{deviceId}")
    public ResponseEntity<Thermostat> updateThermostat(
            @PathVariable Long deviceId,
            @RequestBody Thermostat thermostatDetails) {

        try {
            Thermostat updatedThermostat =
                    thermostatService.updateThermostat(
                            deviceId,
                            thermostatDetails
                    );

            return ResponseEntity.ok(updatedThermostat);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete thermostat
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteThermostat(
            @PathVariable Long deviceId) {

        try {
            thermostatService.deleteThermostat(deviceId);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}