package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.SmartLight;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.SmartLightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-lights")
public class SmartLightController {

    private final SmartLightService smartLightService;
    private final HomeAuthorizationService authService;

    public SmartLightController(SmartLightService smartLightService, HomeAuthorizationService authService) {
        this.smartLightService = smartLightService;
        this.authService = authService;
    }

    @GetMapping
    public List<SmartLight> getAllSmartLights() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return smartLightService.getAllSmartLights();
        }
        return smartLightService.getSmartLightsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<SmartLight> getSmartLightById(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return smartLightService.getSmartLightById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<SmartLight> createSmartLight(
            @PathVariable Long deviceId,
            @RequestBody SmartLight smartLight) {

        authService.assertCanManageDevice(deviceId);
        try {
            SmartLight savedSmartLight = smartLightService.createSmartLight(deviceId, smartLight);
            return ResponseEntity.ok(savedSmartLight);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<SmartLight> updateSmartLight(
            @PathVariable Long deviceId,
            @RequestBody SmartLight smartLightDetails) {

        authService.assertCanAccessDevice(deviceId);
        try {
            SmartLight updatedSmartLight = smartLightService.updateSmartLight(deviceId, smartLightDetails);
            return ResponseEntity.ok(updatedSmartLight);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteSmartLight(@PathVariable Long deviceId) {
        authService.assertCanManageDevice(deviceId);
        try {
            smartLightService.deleteSmartLight(deviceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}