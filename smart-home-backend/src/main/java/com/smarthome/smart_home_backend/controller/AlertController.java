package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Alert;
import com.smarthome.smart_home_backend.service.AlertService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> getAllAlerts() {
        return alertService.getAllAlerts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(
            @PathVariable Long id) {

        return alertService.getAlertById(id)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/device/{deviceId}")
    public List<Alert> getAlertsByDevice(
            @PathVariable Long deviceId) {

        return alertService.getAlertsByDevice(deviceId);
    }

    @GetMapping("/status/{status}")
    public List<Alert> getAlertsByStatus(
            @PathVariable String status) {

        return alertService.getAlertsByStatus(status);
    }

    @GetMapping("/category/{categoryId}")
    public List<Alert> getAlertsByCategory(
            @PathVariable Long categoryId) {

        return alertService.getAlertsByCategory(categoryId);
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Alert> createAlert(
            @PathVariable Long deviceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long ruleId,
            @RequestBody Alert alert) {

        try {
            return ResponseEntity.ok(
                    alertService.createAlert(
                            deviceId,
                            categoryId,
                            ruleId,
                            alert
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alert> updateAlert(
            @PathVariable Long id,
            @RequestBody Alert details) {

        try {
            return ResponseEntity.ok(
                    alertService.updateAlert(id, details)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{alertId}/acknowledge/{userId}")
    public ResponseEntity<Alert> acknowledgeAlert(
            @PathVariable Long alertId,
            @PathVariable Long userId) {

        try {
            return ResponseEntity.ok(
                    alertService.acknowledgeAlert(
                            alertId,
                            userId
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id) {

        try {
            alertService.deleteAlert(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}