package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.SmartLight;
import com.smarthome.smart_home_backend.service.SmartLightService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-lights")
public class SmartLightController {

    private final SmartLightService smartLightService;

    public SmartLightController(SmartLightService smartLightService) {
        this.smartLightService = smartLightService;
    }

    // Get all smart lights
    @GetMapping
    public List<SmartLight> getAllSmartLights() {
        return smartLightService.getAllSmartLights();
    }

    // Get smart light by device ID
    @GetMapping("/{deviceId}")
    public ResponseEntity<SmartLight> getSmartLightById(
            @PathVariable Long deviceId) {

        return smartLightService.getSmartLightById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Create smart light for an existing device
    @PostMapping("/device/{deviceId}")
    public ResponseEntity<SmartLight> createSmartLight(
            @PathVariable Long deviceId,
            @RequestBody SmartLight smartLight) {

        try {
            SmartLight savedSmartLight =
                    smartLightService.createSmartLight(deviceId, smartLight);

            return ResponseEntity.ok(savedSmartLight);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Update smart light
    @PutMapping("/{deviceId}")
    public ResponseEntity<SmartLight> updateSmartLight(
            @PathVariable Long deviceId,
            @RequestBody SmartLight smartLightDetails) {

        try {
            SmartLight updatedSmartLight =
                    smartLightService.updateSmartLight(
                            deviceId,
                            smartLightDetails
                    );

            return ResponseEntity.ok(updatedSmartLight);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete smart light
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteSmartLight(
            @PathVariable Long deviceId) {

        try {
            smartLightService.deleteSmartLight(deviceId);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}