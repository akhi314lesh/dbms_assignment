package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Alert;
import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.SensorReading;

import com.smarthome.smart_home_backend.repository.AlertRepository;
import com.smarthome.smart_home_backend.repository.AutomationRuleRepository;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.SensorReadingRepository;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final SensorReadingRepository readingRepository;
    private final AutomationRuleRepository ruleRepository;

    public DashboardController(
            DeviceRepository deviceRepository,
            AlertRepository alertRepository,
            SensorReadingRepository readingRepository,
            AutomationRuleRepository ruleRepository) {

        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
        this.readingRepository = readingRepository;
        this.ruleRepository = ruleRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        List<Device> devices = deviceRepository.findAll();
        List<Alert> alerts = alertRepository.findAll();
        List<AutomationRule> rules = ruleRepository.findAll();

        long activeDevices = devices.stream()
                .filter(d -> "ON".equalsIgnoreCase(d.getStatus()))
                .count();

        long offlineDevices = devices.stream()
                .filter(d -> "OFFLINE".equalsIgnoreCase(d.getStatus()))
                .count();

        Map<String, Object> summary = new HashMap<>();

        summary.put("totalDevices", devices.size());
        summary.put("activeDevices", activeDevices);
        summary.put("offlineDevices", offlineDevices);
        summary.put("totalAlerts", alerts.size());
        summary.put("totalAutomationRules", rules.size());

        return summary;
    }

    @GetMapping("/device-status")
    public Map<String, Long> getDeviceStatus() {

        List<Device> devices = deviceRepository.findAll();

        Map<String, Long> status = new HashMap<>();

        for (Device device : devices) {

            String deviceStatus = device.getStatus();

            status.put(
                    deviceStatus,
                    status.getOrDefault(deviceStatus, 0L) + 1
            );
        }

        return status;
    }

    @GetMapping("/sensor-trends")
    public List<SensorReading> getSensorTrends() {

        return readingRepository.findAll();
    }

    @GetMapping("/recent-alerts")
    public List<Alert> getRecentAlerts() {

        List<Alert> alerts = alertRepository.findAll();

        return alerts.stream()
                .sorted(
                        (a, b) -> b.getAlertTime()
                                .compareTo(a.getAlertTime())
                )
                .limit(10)
                .toList();
    }

    @GetMapping("/automation-activity")
    public List<AutomationRule> getAutomationActivity() {

        return ruleRepository.findAll();
    }
}