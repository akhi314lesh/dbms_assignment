package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.AutomationRuleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class AutomationRuleController {

    private final AutomationRuleService ruleService;
    private final HomeAuthorizationService authService;

    public AutomationRuleController(
            AutomationRuleService ruleService,
            HomeAuthorizationService authService) {

        this.ruleService = ruleService;
        this.authService = authService;
    }

    @GetMapping
    public List<AutomationRule> getAllRules() {
        User caller = authService.requireCurrentUser();
        if (authService.isAdmin(caller)) {
            return ruleService.getAllRules();
        }
        return ruleService.getRulesByHomeIdsOrUserId(authService.getAccessibleHomeIds(caller), caller.getUserId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutomationRule> getRuleById(@PathVariable Long id) {
        authService.assertCanAccessRule(id);
        return ruleService.getRuleById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<AutomationRule> getRulesByUser(@PathVariable Long userId) {
        authService.assertCanAccessUserProfile(userId);
        return ruleService.getRulesByUser(userId);
    }

    @GetMapping("/device/{deviceId}")
    public List<AutomationRule> getRulesByDevice(@PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return ruleService.getRulesByDevice(deviceId);
    }

    @PostMapping("/device/{deviceId}/user/{userId}")
    public ResponseEntity<AutomationRule> createRule(
            @PathVariable Long deviceId,
            @PathVariable Long userId,
            @RequestBody AutomationRule rule) {

        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !caller.getUserId().equals(userId)) {
            throw new AccessDeniedException("Cannot create automation rule on behalf of another user.");
        }
        authService.assertCanAccessDevice(deviceId);

        try {
            return ResponseEntity.ok(
                    ruleService.createRule(deviceId, userId, rule)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AutomationRule> updateRule(
            @PathVariable Long id,
            @RequestBody AutomationRule details) {

        authService.assertCanAccessRule(id);
        if (details.getConditionDevice() != null && details.getConditionDevice().getDeviceId() != null) {
            authService.assertCanAccessDevice(details.getConditionDevice().getDeviceId());
        }

        try {
            return ResponseEntity.ok(
                    ruleService.updateRule(id, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        authService.assertCanAccessRule(id);
        try {
            ruleService.deleteRule(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}