package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.RuleAction;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.RuleActionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleActionController {

    private final RuleActionService actionService;
    private final HomeAuthorizationService authService;

    public RuleActionController(
            RuleActionService actionService,
            HomeAuthorizationService authService) {
        this.actionService = actionService;
        this.authService = authService;
    }

    @GetMapping("/actions")
    public List<RuleAction> getAllActions() {
        User user = authService.getCurrentUser();
        if (authService.isAdmin(user)) {
            return actionService.getAllActions();
        }
        return actionService.getActionsByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/actions/{id}")
    public ResponseEntity<RuleAction> getActionById(
            @PathVariable Long id) {
        authService.assertCanAccessAction(id);
        return actionService.getActionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/{ruleId}/actions")
    public List<RuleAction> getActionsByRule(
            @PathVariable Long ruleId) {
        authService.assertCanAccessRule(ruleId);
        return actionService.getActionsByRule(ruleId);
    }

    @GetMapping("/actions/device/{deviceId}")
    public List<RuleAction> getActionsByDevice(
            @PathVariable Long deviceId) {
        authService.assertCanAccessDevice(deviceId);
        return actionService.getActionsByDevice(deviceId);
    }

    @PostMapping("/{ruleId}/actions/device/{deviceId}")
    public ResponseEntity<RuleAction> createAction(
            @PathVariable Long ruleId,
            @PathVariable Long deviceId,
            @RequestBody RuleAction action) {
        authService.assertCanAccessRule(ruleId);
        authService.assertCanAccessDevice(deviceId);

        try {
            return ResponseEntity.ok(
                    actionService.createAction(
                            ruleId,
                            deviceId,
                            action
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/actions/{id}")
    public ResponseEntity<RuleAction> updateAction(
            @PathVariable Long id,
            @RequestBody RuleAction details) {
        authService.assertCanAccessAction(id);

        try {
            return ResponseEntity.ok(
                    actionService.updateAction(id, details)
            );

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/actions/{id}")
    public ResponseEntity<Void> deleteAction(
            @PathVariable Long id) {
        authService.assertCanAccessAction(id);

        try {
            actionService.deleteAction(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}