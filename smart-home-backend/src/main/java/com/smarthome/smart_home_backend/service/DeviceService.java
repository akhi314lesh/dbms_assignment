package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.Room;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.RoomRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;

    // Constructor
    public DeviceService(DeviceRepository deviceRepository,
                         RoomRepository roomRepository) {

        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
    }

    // Get all devices
    public List<Device> getAllDevices() {

        return deviceRepository.findAll();
    }

    // Get device by ID
    public Optional<Device> getDeviceById(Long id) {

        return deviceRepository.findById(id);
    }

    // Get devices by room ID
    public List<Device> getDevicesByRoomId(Long roomId) {

        return deviceRepository.findByRoomRoomId(roomId);
    }

    // Get devices by subtype
    public List<Device> getDevicesBySubtype(String subtype) {

        return deviceRepository.findByDeviceSubtype(subtype);
    }

    // Get devices by status
    public List<Device> getDevicesByStatus(String status) {

        return deviceRepository.findByStatus(status);
    }

    // Create device
    public Device createDevice(Long roomId, Device device) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new RuntimeException("Room not found"));

        device.setRoom(room);

        return deviceRepository.save(device);
    }

    // Update device
    public Device updateDevice(Long id, Device deviceDetails) {

        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Device not found"));

        existingDevice.setDeviceName(
                deviceDetails.getDeviceName()
        );

        existingDevice.setDeviceSubtype(
                deviceDetails.getDeviceSubtype()
        );

        existingDevice.setStatus(
                deviceDetails.getStatus()
        );

        existingDevice.setInstallDate(
                deviceDetails.getInstallDate()
        );

        existingDevice.setLastRestartTime(
                deviceDetails.getLastRestartTime()
        );

        // Update room
        if (deviceDetails.getRoom() != null &&
                deviceDetails.getRoom().getRoomId() != null) {

            Long roomId =
                    deviceDetails.getRoom().getRoomId();

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() ->
                            new RuntimeException("Room not found"));

            existingDevice.setRoom(room);
        }

        // Update parent device
        if (deviceDetails.getParentDevice() != null &&
                deviceDetails.getParentDevice().getDeviceId() != null) {

            Long parentId =
                    deviceDetails.getParentDevice().getDeviceId();

            // A device cannot be its own parent
            if (parentId.equals(id)) {

                throw new RuntimeException(
                        "A device cannot be its own parent"
                );
            }

            Device parentDevice =
                    deviceRepository.findById(parentId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Parent device not found"
                                    )
                            );

            existingDevice.setParentDevice(parentDevice);
        }

        return deviceRepository.save(existingDevice);
    }

    // Delete device
    public void deleteDevice(Long id) {

        deviceRepository.deleteById(id);
    }

    // Calculate device uptime
    public String getUptime(Long id) {

        Device device = deviceRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Device not found"));

        LocalDateTime restartTime =
                device.getLastRestartTime();

        if (restartTime == null) {

            return "Uptime unavailable";
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (restartTime.isAfter(now)) {

            return "Invalid restart time";
        }

        Duration duration =
                Duration.between(restartTime, now);

        long days =
                duration.toDays();

        long hours =
                duration.toHours() % 24;

        long minutes =
                duration.toMinutes() % 60;

        return days + " days, "
                + hours + " hours, "
                + minutes + " minutes";
    }
}