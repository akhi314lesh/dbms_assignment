package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.dto.DeviceDetailedDto;
import com.smarthome.smart_home_backend.dto.DeviceStatusDto;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final HomeAuthorizationService authService;

    public DeviceController(DeviceService deviceService, HomeAuthorizationService authService) {
        this.deviceService = deviceService;
        this.authService = authService;
    }

    @GetMapping
    public List<Device> getAllDevices() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return deviceService.getAllDevices();
        }
        return deviceService.getDevicesByHomeIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/home/{homeId}/detailed")
    public List<DeviceDetailedDto> getDetailedDevicesByHome(@PathVariable Long homeId) {
        authService.assertCanAccessHome(homeId);
        return deviceService.getDetailedDevicesByHome(homeId);
    }

    /**
     * GET /api/devices/subtype/{subtype}/detailed
     * Returns full device details for a specific subtype.
     * Enforces strict allowlist: only SMART_LIGHT, THERMOSTAT, TEMPERATURE_SENSOR, MOTION_SENSOR, CAMERA.
     * Admin sees all; USER sees only accessible homes.
     */
    @GetMapping("/subtype/{subtype}/detailed")
    public ResponseEntity<?> getDetailedDevicesBySubtype(@PathVariable String subtype) {
        String normalized = subtype.toUpperCase();
        if (!DeviceService.ALLOWED_SUBTYPES.contains(normalized)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid subtype: " + subtype + ". Allowed: SMART_LIGHT, THERMOSTAT, TEMPERATURE_SENSOR, MOTION_SENSOR, CAMERA"));
        }
        User user = authService.requireCurrentUser();
        boolean isAdmin = authService.isAdmin(user);
        List<DeviceDetailedDto> result = deviceService.getDetailedDevicesBySubtype(
                normalized, isAdmin, isAdmin ? null : authService.getAccessibleHomeIds(user)
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(@PathVariable Long id) {
        authService.assertCanAccessDevice(id);
        return deviceService.getDeviceById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/room/{roomId}")
    public List<Device> getDevicesByRoom(@PathVariable Long roomId) {
        authService.assertCanAccessRoom(roomId);
        return deviceService.getDevicesByRoomId(roomId);
    }

    @GetMapping("/subtype/{subtype}")
    public List<Device> getDevicesBySubtype(@PathVariable String subtype) {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return deviceService.getDevicesBySubtype(subtype);
        }
        return deviceService.getDevicesBySubtypeAndHomeIds(subtype, authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/status/{status}")
    public List<Device> getDevicesByStatus(@PathVariable String status) {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return deviceService.getDevicesByStatus(status);
        }
        return deviceService.getDevicesByStatusAndHomeIds(status, authService.getAccessibleHomeIds(user));
    }

    @PostMapping("/room/{roomId}")
    public ResponseEntity<Device> createDevice(
            @PathVariable Long roomId,
            @RequestBody Device device) {

        authService.assertCanManageRoom(roomId);
        try {
            Device savedDevice = deviceService.createDevice(roomId, device);
            return ResponseEntity.ok(savedDevice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(
            @PathVariable Long id,
            @RequestBody Device deviceDetails) {

        authService.assertCanAccessDevice(id);
        try {
            Device updatedDevice = deviceService.updateDevice(id, deviceDetails);
            return ResponseEntity.ok(updatedDevice);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        authService.assertCanManageDevice(id);
        try {
            deviceService.deleteDevice(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/uptime")
    public ResponseEntity<String> getUptime(@PathVariable Long id) {
        authService.assertCanAccessDevice(id);
        try {
            String uptime = deviceService.getUptime(id);
            return ResponseEntity.ok(uptime);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/devices/{id}/status
     * Updates device status. Only ONLINE, OFFLINE, ERROR are valid (canonical Oracle domain).
     * Returns DeviceStatusDto — does NOT expose raw entity.
     * USER must have access to the device's home. ADMIN can update any.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> patchDeviceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "status field is required"));
        }

        String normalized = status.toUpperCase();
        if (!deviceService.isValidStatus(normalized)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid status: '" + status + "'. Allowed values: ONLINE, OFFLINE, ERROR"));
        }

        // Authorization: user must be able to access the device's home
        try {
            authService.assertCanAccessDevice(id);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied to device " + id));
        }

        try {
            DeviceStatusDto result = deviceService.updateDeviceStatus(id, normalized);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}