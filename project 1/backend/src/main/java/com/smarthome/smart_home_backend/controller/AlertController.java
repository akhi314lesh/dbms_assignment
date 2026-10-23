package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Alert;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;
    private final HomeAuthorizationService authService;

    public AlertController(AlertService alertService, HomeAuthorizationService authService) {
        this.alertService = alertService;
        this.authService = authService;
    }

    @GetMapping
    public List<Alert> getAllAlerts() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return alertService.getAllAlerts();
        }
        return alertService.getAlertsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        authService.assertCanAccessAlert(id);
        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/device/{deviceId}")
    public List<Alert> getAlertsByDevice(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return alertService.getAlertsByDevice(deviceId);
    }

    @GetMapping("/status/{status}")
    public List<Alert> getAlertsByStatus(@PathVariable String status) {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return alertService.getAlertsByStatus(status);
        }
        return alertService.getAlertsByStatusAndHomeIds(status, authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/category/{categoryId}")
    public List<Alert> getAlertsByCategory(@PathVariable Long categoryId) {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return alertService.getAlertsByCategory(categoryId);
        }
        return alertService.getAlertsByCategoryAndHomeIds(categoryId, authService.getAccessibleHomeIds(user));
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Alert> createAlert(
            @PathVariable Long deviceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long ruleId,
            @RequestBody Alert alert) {

        authService.assertCanAccessDevice(deviceId);
        try {
            return ResponseEntity.ok(
                    alertService.createAlert(deviceId, categoryId, ruleId, alert)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alert> updateAlert(
            @PathVariable Long id,
            @RequestBody Alert details) {

        authService.assertCanAccessAlert(id);
        try {
            return ResponseEntity.ok(alertService.updateAlert(id, details));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{alertId}/acknowledge/{userId}")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable Long alertId,
            @PathVariable Long userId) {

        User caller = authService.requireCurrentUser();
        authService.assertCanAccessAlert(alertId);

        if (!authService.isAdmin(caller) && !caller.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot acknowledge alert on behalf of another user.");
        }

        try {
            return ResponseEntity.ok(alertService.acknowledgeAlert(alertId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledgeAlertForCurrentUser(@PathVariable Long id) {
        User caller = authService.requireCurrentUser();
        authService.assertCanAccessAlert(id);
        try {
            return ResponseEntity.ok(alertService.acknowledgeAlert(id, caller.getUserId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        authService.assertCanManageAlert(id);
        try {
            alertService.deleteAlert(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}