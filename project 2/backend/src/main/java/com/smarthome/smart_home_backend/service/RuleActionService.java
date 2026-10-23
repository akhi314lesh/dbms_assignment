package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.RuleAction;
import com.smarthome.smart_home_backend.repository.AutomationRuleRepository;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.RuleActionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RuleActionService {

    private final RuleActionRepository actionRepository;
    private final AutomationRuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;

    public RuleActionService(
            RuleActionRepository actionRepository,
            AutomationRuleRepository ruleRepository,
            DeviceRepository deviceRepository) {

        this.actionRepository = actionRepository;
        this.ruleRepository = ruleRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<RuleAction> getAllActions() {
        return actionRepository.findAll();
    }

    public Optional<RuleAction> getActionById(Long id) {
        return actionRepository.findById(id);
    }

    public List<RuleAction> getActionsByRule(Long ruleId) {
        return actionRepository.findByRuleRuleId(ruleId);
    }

    public List<RuleAction> getActionsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return actionRepository.findByTargetDeviceRoomHomeHomeIdIn(homeIds);
    }

    public List<RuleAction> getActionsByDevice(Long deviceId) {
        return actionRepository.findByTargetDeviceDeviceId(deviceId);
    }

    @Transactional
    public RuleAction createAction(
            Long ruleId,
            Long deviceId,
            RuleAction action) {

        AutomationRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(
                        () -> new RuntimeException("Rule not found")
                );

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        action.setRule(rule);
        action.setTargetDevice(device);

        return actionRepository.save(action);
    }

    @Transactional
    public RuleAction updateAction(
            Long id,
            RuleAction details) {

        RuleAction existing =
                actionRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Rule action not found"
                                )
                        );

        existing.setActionType(
                details.getActionType()
        );

        existing.setActionValue(
                details.getActionValue()
        );

        return actionRepository.save(existing);
    }

    @Transactional
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }
}