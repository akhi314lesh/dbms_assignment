package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.service.DeviceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    // Constructor
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // ---------------------------------------------------------
    // 1. GET ALL DEVICES
    // ---------------------------------------------------------

    @GetMapping
    public List<Device> getAllDevices() {
        return deviceService.getAllDevices();
    }

    // ---------------------------------------------------------
    // 2. GET DEVICE BY ID
    // ---------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<Device> getDeviceById(
            @PathVariable Long id) {

        return deviceService.getDeviceById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------
    // 3. GET DEVICES BY ROOM ID
    // ---------------------------------------------------------

    @GetMapping("/room/{roomId}")
    public List<Device> getDevicesByRoom(
            @PathVariable Long roomId) {

        return deviceService.getDevicesByRoomId(roomId);
    }

    // ---------------------------------------------------------
    // 4. GET DEVICES BY SUBTYPE
    // ---------------------------------------------------------

    @GetMapping("/subtype/{subtype}")
    public List<Device> getDevicesBySubtype(
            @PathVariable String subtype) {

        return deviceService.getDevicesBySubtype(subtype);
    }

    // ---------------------------------------------------------
    // 5. GET DEVICES BY STATUS
    // ---------------------------------------------------------

    @GetMapping("/status/{status}")
    public List<Device> getDevicesByStatus(
            @PathVariable String status) {

        return deviceService.getDevicesByStatus(status);
    }

    // ---------------------------------------------------------
    // 6. CREATE DEVICE
    // ---------------------------------------------------------

    @PostMapping("/room/{roomId}")
    public ResponseEntity<Device> createDevice(
            @PathVariable Long roomId,
            @RequestBody Device device) {

        try {

            Device savedDevice =
                    deviceService.createDevice(roomId, device);

            return ResponseEntity.ok(savedDevice);

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ---------------------------------------------------------
    // 7. UPDATE DEVICE
    // ---------------------------------------------------------

    @PutMapping("/{id}")
    public ResponseEntity<Device> updateDevice(
            @PathVariable Long id,
            @RequestBody Device deviceDetails) {

        try {

            Device updatedDevice =
                    deviceService.updateDevice(id, deviceDetails);

            return ResponseEntity.ok(updatedDevice);

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ---------------------------------------------------------
    // 8. DELETE DEVICE
    // ---------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable Long id) {

        try {

            deviceService.deleteDevice(id);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }

    // ---------------------------------------------------------
    // 9. GET DEVICE UPTIME
    // ---------------------------------------------------------

    @GetMapping("/{id}/uptime")
    public ResponseEntity<String> getUptime(
            @PathVariable Long id) {

        try {

            String uptime =
                    deviceService.getUptime(id);

            return ResponseEntity.ok(uptime);

        } catch (RuntimeException e) {

            return ResponseEntity.notFound().build();
        }
    }
}