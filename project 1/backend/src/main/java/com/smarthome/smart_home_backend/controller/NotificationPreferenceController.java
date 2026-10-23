package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.NotificationPreference;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.NotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final HomeAuthorizationService authService;

    public NotificationPreferenceController(
            NotificationPreferenceService preferenceService,
            HomeAuthorizationService authService) {

        this.preferenceService = preferenceService;
        this.authService = authService;
    }

    @GetMapping
    public List<NotificationPreference> getAllPreferences() {
        User caller = authService.requireCurrentUser();
        if (authService.isAdmin(caller)) {
            return preferenceService.getAllPreferences();
        }
        return preferenceService.getByUser(caller.getUserId());
    }

    @GetMapping("/user/{userId}")
    public List<NotificationPreference> getByUser(@PathVariable Long userId) {
        authService.assertCanAccessUserProfile(userId);
        return preferenceService.getByUser(userId);
    }

    @GetMapping("/device/{deviceId}")
    public List<NotificationPreference> getByDevice(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return preferenceService.getByDevice(deviceId);
    }

    @GetMapping("/category/{categoryId}")
    public List<NotificationPreference> getByCategory(@PathVariable Long categoryId) {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller)) {
            throw new AccessDeniedException("Access is denied. Admin role required to query preferences by category.");
        }
        return preferenceService.getByCategory(categoryId);
    }

    @GetMapping("/{userId}/{deviceId}/{categoryId}")
    public ResponseEntity<NotificationPreference> getPreference(
            @PathVariable Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long categoryId) {

        authService.assertCanAccessPreference(userId, deviceId);
        return preferenceService
                .getPreference(userId, deviceId, categoryId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/{deviceId}/{categoryId}")
    public ResponseEntity<NotificationPreference> createPreference(
            @PathVariable Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long categoryId,
            @RequestBody NotificationPreference preference) {

        authService.assertCanAccessPreference(userId, deviceId);
        try {
            return ResponseEntity.ok(
                    preferenceService.createPreference(userId, deviceId, categoryId, preference)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{userId}/{deviceId}/{categoryId}")
    public ResponseEntity<NotificationPreference> updatePreference(
            @PathVariable Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long categoryId,
            @RequestBody NotificationPreference details) {

        authService.assertCanAccessPreference(userId, deviceId);
        try {
            return ResponseEntity.ok(
                    preferenceService.updatePreference(userId, deviceId, categoryId, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/{deviceId}/{categoryId}")
    public ResponseEntity<Void> deletePreference(
            @PathVariable Long userId,
            @PathVariable Long deviceId,
            @PathVariable Long categoryId) {

        authService.assertCanAccessPreference(userId, deviceId);
        try {
            preferenceService.deletePreference(userId, deviceId, categoryId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}