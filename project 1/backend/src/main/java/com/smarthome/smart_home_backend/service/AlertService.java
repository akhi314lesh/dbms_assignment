package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.*;
import com.smarthome.smart_home_backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AlertRepository alertRepository;
    private final DeviceRepository deviceRepository;
    private final AlertCategoryRepository categoryRepository;
    private final AutomationRuleRepository ruleRepository;
    private final UserRepository userRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public AlertService(
            AlertRepository alertRepository,
            DeviceRepository deviceRepository,
            AlertCategoryRepository categoryRepository,
            AutomationRuleRepository ruleRepository,
            UserRepository userRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {

        this.alertRepository = alertRepository;
        this.deviceRepository = deviceRepository;
        this.categoryRepository = categoryRepository;
        this.ruleRepository = ruleRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public List<Alert> getAlertsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return alertRepository.findByDeviceRoomHomeHomeIdIn(homeIds);
    }

    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    public List<Alert> getAlertsByDevice(Long deviceId) {
        return alertRepository.findByDeviceDeviceId(deviceId);
    }

    public List<Alert> getAlertsByDeviceAndHomeIds(Long deviceId, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return alertRepository.findByDeviceDeviceIdAndDeviceRoomHomeHomeIdIn(deviceId, homeIds);
    }

    public List<Alert> getAlertsByStatus(String status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> getAlertsByStatusAndHomeIds(String status, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return alertRepository.findByStatusAndDeviceRoomHomeHomeIdIn(status, homeIds);
    }

    public List<Alert> getAlertsByCategory(Long categoryId) {
        return alertRepository.findByCategoryCategoryId(categoryId);
    }

    public List<Alert> getAlertsByCategoryAndHomeIds(Long categoryId, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return alertRepository.findByCategoryCategoryIdAndDeviceRoomHomeHomeIdIn(categoryId, homeIds);
    }

    @Transactional
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

    @Transactional
    public Alert acknowledgeAlert(
            Long alertId,
            Long userId) {

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            jdbcTemplate.update("CALL sp_acknowledge_alert(?, ?)", alertId, userId);
            if (entityManager != null) {
                entityManager.clear();
            }
        } catch (org.springframework.dao.DataAccessException e) {
            if (e.getMessage() != null && e.getMessage().contains("20003")) {
                return alert;
            }
            throw e;
        }

        return alertRepository.findById(alertId)
                .orElse(alert);
    }

    @Transactional
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

    @Transactional
    public void deleteAlert(Long id) {
        alertRepository.deleteById(id);
    }
}