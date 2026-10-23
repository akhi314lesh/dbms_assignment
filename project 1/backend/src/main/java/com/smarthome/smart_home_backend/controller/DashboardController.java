package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Alert;
import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/device-status")
    public Map<String, Long> getDeviceStatus() {
        return dashboardService.getDeviceStatus();
    }

    @GetMapping("/sensor-trends")
    public List<SensorReading> getSensorTrends() {
        return dashboardService.getSensorTrends();
    }

    @GetMapping("/recent-alerts")
    public List<Alert> getRecentAlerts() {
        return dashboardService.getRecentAlerts();
    }

    @GetMapping("/automation-activity")
    public List<AutomationRule> getAutomationActivity() {
        return dashboardService.getAutomationActivity();
    }
}