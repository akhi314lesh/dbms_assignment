package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.*;
import com.smarthome.smart_home_backend.repository.*;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final AlertCategoryRepository categoryRepository;
    private final AutomationRuleRepository ruleRepository;
    private final UserRepository userRepository;

    public AlertService(
            AlertRepository alertRepository,
            DeviceRepository deviceRepository,
            AlertCategoryRepository categoryRepository,
            AutomationRuleRepository ruleRepository,
            UserRepository userRepository) {

        this.alertRepository = alertRepository;
        this.deviceRepository = deviceRepository;
        this.categoryRepository = categoryRepository;
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<Alert> getAlertsByDevice(Long deviceId) {
        return alertRepository.findByDeviceDeviceId(deviceId);
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> getAlertsByCategory(Long categoryId) {
        return alertRepository.findByCategoryCategoryId(categoryId);
    }

    public Alert createAlert(
            Long deviceId,
            Long categoryId,
            Long ruleId,
            Alert alert) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        alert.setDevice(device);

        if (categoryId != null) {
            AlertCategory category =
                    categoryRepository.findById(categoryId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Alert category not found"
                                    )
                            );

            alert.setCategory(category);
        }

        if (ruleId != null) {
            AutomationRule rule =
                    ruleRepository.findById(ruleId)
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Automation rule not found"
                                    )
                            );

            alert.setTriggeredByRule(rule);
        }

        return alertRepository.save(alert);
    }

    public Alert acknowledgeAlert(
            Long alertId,
            Long userId) {

        Alert alert =
                alertRepository.findById(alertId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Alert not found"
                                )
                        );

        User user =
                userRepository.findById(userId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "User not found"
                                )
                        );

        alert.setAcknowledgedBy(user);
        alert.setAcknowledgedTimestamp(
                LocalDateTime.now()
        );
        alert.setStatus("ACKNOWLEDGED");

        return alertRepository.save(alert);
    }

    public Alert updateAlert(
            Long id,
            Alert details) {

        Alert existing =
                alertRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Alert not found"
                                )
                        );

        existing.setMessage(details.getMessage());
        existing.setAlertTime(details.getAlertTime());
        existing.setSource(details.getSource());
        existing.setStatus(details.getStatus());

        return alertRepository.save(existing);
    }

    public void deleteAlert(Long id) {
        alertRepository.deleteById(id);
    }
}