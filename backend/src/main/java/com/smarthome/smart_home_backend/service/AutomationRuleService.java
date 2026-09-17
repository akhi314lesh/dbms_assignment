package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.AutomationRuleRepository;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AutomationRuleService {

    private final AutomationRuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    public AutomationRuleService(
            AutomationRuleRepository ruleRepository,
            DeviceRepository deviceRepository,
            UserRepository userRepository) {

        this.ruleRepository = ruleRepository;
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
    }

    public List<AutomationRule> getAllRules() {
        return ruleRepository.findAll();
    }

    public Optional<AutomationRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    public List<AutomationRule> getRulesByUser(Long userId) {
        return ruleRepository.findByCreatedByUserUserId(userId);
    }

    public List<AutomationRule> getRulesByDevice(Long deviceId) {
        return ruleRepository.findByConditionDeviceDeviceId(deviceId);
    }

    public AutomationRule createRule(
            Long deviceId,
            Long userId,
            AutomationRule rule) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        rule.setConditionDevice(device);
        rule.setCreatedByUser(user);

        return ruleRepository.save(rule);
    }

    public AutomationRule updateRule(
            Long id,
            AutomationRule details) {

        AutomationRule existing =
                ruleRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Automation rule not found"
                                )
                        );

        existing.setRuleName(details.getRuleName());
        existing.setConditionOperator(
                details.getConditionOperator()
        );
        existing.setConditionValue(
                details.getConditionValue()
        );
        existing.setCreatedDate(
                details.getCreatedDate()
        );

        return ruleRepository.save(existing);
    }

    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }
}