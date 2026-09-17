package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.RuleAction;
import com.smarthome.smart_home_backend.service.RuleActionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleActionController {

    private final RuleActionService actionService;

    public RuleActionController(
            RuleActionService actionService) {
        this.actionService = actionService;
    }

    @GetMapping("/actions")
    public List<RuleAction> getAllActions() {
        return actionService.getAllActions();
    }

    @GetMapping("/actions/{id}")
    public ResponseEntity<RuleAction> getActionById(
            @PathVariable Long id) {

        return actionService.getActionById(id)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/{ruleId}/actions")
    public List<RuleAction> getActionsByRule(
            @PathVariable Long ruleId) {

        return actionService.getActionsByRule(ruleId);
    }

    @GetMapping("/actions/device/{deviceId}")
    public List<RuleAction> getActionsByDevice(
            @PathVariable Long deviceId) {

        return actionService.getActionsByDevice(deviceId);
    }

    @PostMapping("/{ruleId}/actions/device/{deviceId}")
    public ResponseEntity<RuleAction> createAction(
            @PathVariable Long ruleId,
            @PathVariable Long deviceId,
            @RequestBody RuleAction action) {

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

        try {
            actionService.deleteAction(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}