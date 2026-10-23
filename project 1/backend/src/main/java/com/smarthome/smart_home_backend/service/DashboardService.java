package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Alert;
import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.AlertRepository;
import com.smarthome.smart_home_backend.repository.AutomationRuleRepository;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.SensorReadingRepository;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final SensorReadingRepository readingRepository;
    private final AutomationRuleRepository ruleRepository;
    private final com.smarthome.smart_home_backend.repository.RoomRepository roomRepository;
    private final HomeAuthorizationService authService;

    public DashboardService(
            DeviceRepository deviceRepository,
            AlertRepository alertRepository,
            SensorReadingRepository readingRepository,
            AutomationRuleRepository ruleRepository,
            com.smarthome.smart_home_backend.repository.RoomRepository roomRepository,
            HomeAuthorizationService authService) {

        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
        this.readingRepository = readingRepository;
        this.ruleRepository = ruleRepository;
        this.roomRepository = roomRepository;
        this.authService = authService;
    }

    public Map<String, Object> getSummary() {
        User user = authService.requireCurrentUser();
        Map<String, Object> summary = new HashMap<>();

        if (authService.isAdmin(user)) {
            List<Device> devices = deviceRepository.findAll();
            List<Alert> alerts = alertRepository.findAll();
            List<AutomationRule> rules = ruleRepository.findAll();
            long totalHomes = authService.getAccessibleHomeIds(user).size();
            long totalRooms = roomRepository.count();

            long activeDevices = devices.stream()
                    .filter(d -> "ON".equalsIgnoreCase(d.getStatus()) || "ONLINE".equalsIgnoreCase(d.getStatus()))
                    .count();

            long offlineDevices = devices.stream()
                    .filter(d -> "OFFLINE".equalsIgnoreCase(d.getStatus()))
                    .count();

            summary.put("totalHomes", totalHomes);
            summary.put("totalRooms", totalRooms);
            summary.put("totalDevices", devices.size());
            summary.put("activeDevices", activeDevices);
            summary.put("offlineDevices", offlineDevices);
            summary.put("totalAlerts", alerts.size());
            summary.put("totalAutomationRules", rules.size());

            Map<String, Long> subtypeCounts = devices.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            d -> d.getDeviceSubtype() != null ? d.getDeviceSubtype().toUpperCase().replace("_", "").replace(" ", "") : "OTHER",
                            java.util.stream.Collectors.counting()
                    ));
            Map<String, Long> breakdown = new HashMap<>();
            breakdown.put("smartLights", subtypeCounts.getOrDefault("SMARTLIGHT", 0L));
            breakdown.put("thermostats", subtypeCounts.getOrDefault("THERMOSTAT", 0L));
            breakdown.put("temperatureSensors", subtypeCounts.getOrDefault("TEMPERATURESENSOR", 0L));
            breakdown.put("motionSensors", subtypeCounts.getOrDefault("MOTIONSENSOR", 0L));
            breakdown.put("cameras", subtypeCounts.getOrDefault("CAMERA", 0L));
            breakdown.put("hubs", subtypeCounts.getOrDefault("HUB", 0L));
            summary.put("deviceBreakdown", breakdown);

            return summary;
        }

        List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(user);
        if (accessibleHomeIds.isEmpty()) {
            summary.put("totalHomes", 0);
            summary.put("totalRooms", 0);
            summary.put("totalDevices", 0);
            summary.put("activeDevices", 0L);
            summary.put("offlineDevices", 0L);
            summary.put("totalAlerts", 0);
            summary.put("totalAutomationRules", 0);
            summary.put("deviceBreakdown", Map.of(
                    "smartLights", 0L,
                    "thermostats", 0L,
                    "temperatureSensors", 0L,
                    "motionSensors", 0L,
                    "cameras", 0L,
                    "hubs", 0L
            ));
            return summary;
        }

        List<Device> devices = deviceRepository.findByRoomHomeHomeIdIn(accessibleHomeIds);
        List<Alert> alerts = alertRepository.findByDeviceRoomHomeHomeIdIn(accessibleHomeIds);
        List<AutomationRule> rules = ruleRepository.findByConditionDeviceRoomHomeHomeIdIn(accessibleHomeIds);
        List<com.smarthome.smart_home_backend.entity.Room> rooms = roomRepository.findByHomeHomeIdIn(accessibleHomeIds);

        long activeDevices = devices.stream()
                .filter(d -> "ON".equalsIgnoreCase(d.getStatus()) || "ONLINE".equalsIgnoreCase(d.getStatus()))
                .count();

        long offlineDevices = devices.stream()
                .filter(d -> "OFFLINE".equalsIgnoreCase(d.getStatus()))
                .count();

        summary.put("totalHomes", accessibleHomeIds.size());
        summary.put("totalRooms", rooms.size());
        summary.put("totalDevices", devices.size());
        summary.put("activeDevices", activeDevices);
        summary.put("offlineDevices", offlineDevices);
        summary.put("totalAlerts", alerts.size());
        summary.put("totalAutomationRules", rules.size());

        Map<String, Long> subtypeCounts = devices.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        d -> d.getDeviceSubtype() != null ? d.getDeviceSubtype().toUpperCase().replace("_", "").replace(" ", "") : "OTHER",
                        java.util.stream.Collectors.counting()
                ));
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("smartLights", subtypeCounts.getOrDefault("SMARTLIGHT", 0L));
        breakdown.put("thermostats", subtypeCounts.getOrDefault("THERMOSTAT", 0L));
        breakdown.put("temperatureSensors", subtypeCounts.getOrDefault("TEMPERATURESENSOR", 0L));
        breakdown.put("motionSensors", subtypeCounts.getOrDefault("MOTIONSENSOR", 0L));
        breakdown.put("cameras", subtypeCounts.getOrDefault("CAMERA", 0L));
        breakdown.put("hubs", subtypeCounts.getOrDefault("HUB", 0L));
        summary.put("deviceBreakdown", breakdown);

        return summary;
    }

    public Map<String, Long> getDeviceStatus() {
        User user = authService.requireCurrentUser();
        List<Device> devices;

        if (authService.isAdmin(user)) {
            devices = deviceRepository.findAll();
        } else {
            List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(user);
            if (accessibleHomeIds.isEmpty()) {
                return Collections.emptyMap();
            }
            devices = deviceRepository.findByRoomHomeHomeIdIn(accessibleHomeIds);
        }

        Map<String, Long> status = new HashMap<>();
        for (Device device : devices) {
            String deviceStatus = device.getStatus();
            status.put(deviceStatus, status.getOrDefault(deviceStatus, 0L) + 1);
        }
        return status;
    }

    public List<SensorReading> getSensorTrends() {
        User user = authService.requireCurrentUser();

        if (authService.isAdmin(user)) {
            return readingRepository.findAll();
        }

        List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(user);
        if (accessibleHomeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return readingRepository.findByDeviceRoomHomeHomeIdIn(accessibleHomeIds);
    }

    public List<Alert> getRecentAlerts() {
        User user = authService.requireCurrentUser();
        List<Alert> alerts;

        if (authService.isAdmin(user)) {
            alerts = alertRepository.findAll();
        } else {
            List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(user);
            if (accessibleHomeIds.isEmpty()) {
                return Collections.emptyList();
            }
            alerts = alertRepository.findByDeviceRoomHomeHomeIdIn(accessibleHomeIds);
        }

        return alerts.stream()
                .sorted((a, b) -> b.getAlertTime().compareTo(a.getAlertTime()))
                .limit(10)
                .toList();
    }

    public List<AutomationRule> getAutomationActivity() {
        User user = authService.requireCurrentUser();

        if (authService.isAdmin(user)) {
            return ruleRepository.findAll();
        }

        List<Long> accessibleHomeIds = authService.getAccessibleHomeIds(user);
        if (accessibleHomeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return ruleRepository.findByConditionDeviceRoomHomeHomeIdInOrCreatedByUserUserId(
                accessibleHomeIds,
                user.getUserId()
        );
    }
}
