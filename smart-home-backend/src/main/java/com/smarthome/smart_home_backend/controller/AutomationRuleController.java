package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.service.AutomationRuleService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class AutomationRuleController {

    private final AutomationRuleService ruleService;

    public AutomationRuleController(
            AutomationRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    public List<AutomationRule> getAllRules() {
        return ruleService.getAllRules();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutomationRule> getRuleById(
            @PathVariable Long id) {

        return ruleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/user/{userId}")
    public List<AutomationRule> getRulesByUser(
            @PathVariable Long userId) {

        return ruleService.getRulesByUser(userId);
    }

    @GetMapping("/device/{deviceId}")
    public List<AutomationRule> getRulesByDevice(
            @PathVariable Long deviceId) {

        return ruleService.getRulesByDevice(deviceId);
    }

    @PostMapping("/device/{deviceId}/user/{userId}")
    public ResponseEntity<AutomationRule> createRule(
            @PathVariable Long deviceId,
            @PathVariable Long userId,
            @RequestBody AutomationRule rule) {

        try {
            return ResponseEntity.ok(
                    ruleService.createRule(
                            deviceId,
                            userId,
                            rule
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutomationRule> updateRule(
            @PathVariable Long id,
            @RequestBody AutomationRule details) {

        try {
            return ResponseEntity.ok(
                    ruleService.updateRule(id, details)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long id) {

        try {
            ruleService.deleteRule(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}