package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.dto.DeviceDetailedDto;
import com.smarthome.smart_home_backend.dto.DeviceStatusDto;
import com.smarthome.smart_home_backend.entity.*;
import com.smarthome.smart_home_backend.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SmartLightRepository smartLightRepository;
    private final ThermostatRepository thermostatRepository;
    private final TemperatureSensorRepository temperatureSensorRepository;
    private final MotionSensorRepository motionSensorRepository;
    private final CameraRepository cameraRepository;
    private final SensorReadingRepository sensorReadingRepository;

    public DeviceService(DeviceRepository deviceRepository,
                         RoomRepository roomRepository,
                         JdbcTemplate jdbcTemplate,
                         SmartLightRepository smartLightRepository,
                         ThermostatRepository thermostatRepository,
                         TemperatureSensorRepository temperatureSensorRepository,
                         MotionSensorRepository motionSensorRepository,
                         CameraRepository cameraRepository,
                         SensorReadingRepository sensorReadingRepository) {

        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.smartLightRepository = smartLightRepository;
        this.thermostatRepository = thermostatRepository;
        this.temperatureSensorRepository = temperatureSensorRepository;
        this.motionSensorRepository = motionSensorRepository;
        this.cameraRepository = cameraRepository;
        this.sensorReadingRepository = sensorReadingRepository;
    }

    // Get all devices
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    public List<Device> getDevicesByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return deviceRepository.findByRoomHomeHomeIdIn(homeIds);
    }

    // Get device by ID
    public Optional<Device> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    // Get devices by room ID
    public List<Device> getDevicesByRoomId(Long roomId) {
        return deviceRepository.findByRoomRoomId(roomId);
    }

    public List<Device> getDevicesByRoomIdAndHomeIds(Long roomId, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return deviceRepository.findByRoomRoomIdAndRoomHomeHomeIdIn(roomId, homeIds);
    }

    // Get devices by subtype
    public List<Device> getDevicesBySubtype(String subtype) {
        return deviceRepository.findByDeviceSubtype(subtype);
    }

    public List<Device> getDevicesBySubtypeAndHomeIds(String subtype, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return deviceRepository.findByRoomHomeHomeIdInAndDeviceSubtype(homeIds, subtype);
    }

    // Get devices by status
    public List<Device> getDevicesByStatus(String status) {
        return deviceRepository.findByStatus(status);
    }

    public List<Device> getDevicesByStatusAndHomeIds(String status, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return deviceRepository.findByRoomHomeHomeIdInAndStatus(homeIds, status);
    }

    // Create device
    @Transactional
    public Device createDevice(Long roomId, Device device) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new RuntimeException("Room not found"));

        device.setRoom(room);

        return deviceRepository.save(device);
    }

    // Update device
    @Transactional
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
    @Transactional
    public void deleteDevice(Long id) {

        deviceRepository.deleteById(id);
    }

    // Calculate device uptime via Oracle PL/SQL function FN_CALCULATE_DEVICE_UPTIME
    public String getUptime(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT fn_calculate_device_uptime(?) FROM DUAL",
                    String.class,
                    id
            );
        } catch (Exception e) {
            return "Uptime unavailable";
        }
    }

    // ===== ALLOWED STATUS VALUES (canonical Oracle domain) =====
    private static final Set<String> ALLOWED_STATUSES = Set.of("ONLINE", "OFFLINE", "ERROR");

    // ===== ALLOWED SUBTYPE VALUES for the detailed category endpoint =====
    public static final Set<String> ALLOWED_SUBTYPES = Set.of(
            "SMART_LIGHT", "THERMOSTAT", "TEMPERATURE_SENSOR", "MOTION_SENSOR", "CAMERA"
    );

    public boolean isValidStatus(String status) {
        return status != null && ALLOWED_STATUSES.contains(status.toUpperCase());
    }

    /**
     * Maps DeviceDetailedDto fields from a Device entity.
     * Shared helper used by both getDetailedDevicesByHome and getDetailedDevicesBySubtype.
     */
    private DeviceDetailedDto mapToDetailedDto(Device d) {
        DeviceDetailedDto dto = new DeviceDetailedDto();
        dto.setDeviceId(d.getDeviceId());
        dto.setDeviceName(d.getDeviceName());
        dto.setDeviceSubtype(d.getDeviceSubtype());
        dto.setStatus(d.getStatus());
        dto.setInstallDate(d.getInstallDate());
        dto.setLastRestartTime(d.getLastRestartTime());

        if (d.getRoom() != null) {
            dto.setRoomId(d.getRoom().getRoomId());
            dto.setRoomName(d.getRoom().getRoomName());
            dto.setFloorNumber(d.getRoom().getFloorNumber());
            if (d.getRoom().getHome() != null) {
                dto.setHomeId(d.getRoom().getHome().getHomeId());
                dto.setHomeName(d.getRoom().getHome().getHomeName());
            }
        }

        if (d.getParentDevice() != null) {
            dto.setParentDeviceId(d.getParentDevice().getDeviceId());
            dto.setParentDeviceName(d.getParentDevice().getDeviceName());
        }

        dto.setUptime(getUptime(d.getDeviceId()));

        String subtype = d.getDeviceSubtype() != null ? d.getDeviceSubtype().toUpperCase() : "";
        switch (subtype) {
            case "SMART_LIGHT" -> {
                smartLightRepository.findById(d.getDeviceId()).ifPresent(sl -> {
                    dto.setBrightness(sl.getBrightness());
                    dto.setColorSupport(sl.getColorSupport());
                });
            }
            case "THERMOSTAT" -> {
                thermostatRepository.findById(d.getDeviceId()).ifPresent(t -> {
                    dto.setTargetTemperature(t.getTargetTemperature());
                    dto.setThermostatMode(t.getMode());
                });
            }
            case "TEMPERATURE_SENSOR" -> {
                temperatureSensorRepository.findById(d.getDeviceId()).ifPresent(ts -> {
                    dto.setUnit(ts.getUnit());
                    dto.setMinRange(ts.getMinRange());
                    dto.setMaxRange(ts.getMaxRange());
                });
                List<SensorReading> readings = sensorReadingRepository.findByIdDeviceId(d.getDeviceId());
                readings.stream()
                        .filter(r -> r.getReadingTime() != null)
                        .max(Comparator.comparing(SensorReading::getReadingTime))
                        .ifPresent(latest -> {
                            dto.setLatestReadingValue(latest.getValue());
                            dto.setLatestReadingTime(latest.getReadingTime());
                        });
            }
            case "MOTION_SENSOR" -> {
                motionSensorRepository.findById(d.getDeviceId()).ifPresent(ms -> {
                    dto.setSensitivityLevel(ms.getSensitivityLevel());
                    dto.setDetectionRange(ms.getDetectionRange());
                });
            }
            case "CAMERA" -> {
                cameraRepository.findById(d.getDeviceId()).ifPresent(cam -> {
                    dto.setResolution(cam.getResolution());
                    dto.setStorageType(cam.getStorageType());
                    dto.setNightVisionSupport(cam.getNightVision());
                });
            }
            default -> {
                // HUB or other subtypes have base attributes + uptime
            }
        }
        return dto;
    }

    public List<DeviceDetailedDto> getDetailedDevicesByHome(Long homeId) {
        List<Device> devices = deviceRepository.findByRoomHomeHomeIdIn(List.of(homeId));
        return devices.stream().map(this::mapToDetailedDto).collect(Collectors.toList());
    }

    /**
     * Returns detailed DTOs for a given subtype, scoped to accessible homes.
     * adminScope=true means all homes; adminScope=false means filter by accessibleHomeIds.
     */
    public List<DeviceDetailedDto> getDetailedDevicesBySubtype(String subtype, boolean adminScope, Collection<Long> accessibleHomeIds) {
        List<Device> devices;
        if (adminScope) {
            devices = deviceRepository.findByDeviceSubtype(subtype);
        } else {
            if (accessibleHomeIds == null || accessibleHomeIds.isEmpty()) {
                return Collections.emptyList();
            }
            devices = deviceRepository.findByRoomHomeHomeIdInAndDeviceSubtype(accessibleHomeIds, subtype);
        }
        return devices.stream().map(this::mapToDetailedDto).collect(Collectors.toList());
    }

    /**
     * Updates DEVICES.STATUS in Oracle.
     * Caller is responsible for authorization and status validation.
     */
    @Transactional
    public DeviceStatusDto updateDeviceStatus(Long id, String normalizedStatus) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Device not found: " + id));
        device.setStatus(normalizedStatus);
        Device saved = deviceRepository.save(device);
        return new DeviceStatusDto(saved.getDeviceId(), saved.getStatus());
    }
}