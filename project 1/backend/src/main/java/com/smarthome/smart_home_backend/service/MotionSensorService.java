package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.MotionSensor;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.MotionSensorRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MotionSensorService {

    private final MotionSensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public MotionSensorService(
            MotionSensorRepository sensorRepository,
            DeviceRepository deviceRepository,
            jakarta.persistence.EntityManager entityManager) {

        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
        this.entityManager = entityManager;
    }

    public List<MotionSensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    public List<MotionSensor> getSensorsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sensorRepository.findByHomeIds(homeIds);
    }

    public Optional<MotionSensor> getSensorById(Long deviceId) {
        return sensorRepository.findById(deviceId);
    }

    @Transactional
    public MotionSensor createSensor(
            Long deviceId,
            MotionSensor sensor) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!"MOTION_SENSOR".equalsIgnoreCase(
                device.getDeviceSubtype())) {

            throw new RuntimeException(
                    "Device subtype is not MOTION_SENSOR"
            );
        }

        sensor.setDevice(device);
        sensor.setDeviceId(deviceId);

        entityManager.persist(sensor);
        return sensor;
    }

    @Transactional
    public MotionSensor updateSensor(
            Long deviceId,
            MotionSensor details) {

        MotionSensor existing =
                sensorRepository.findById(deviceId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Motion sensor not found"
                                )
                        );

        existing.setSensitivityLevel(
                details.getSensitivityLevel()
        );

        existing.setDetectionRange(
                details.getDetectionRange()
        );

        return sensorRepository.save(existing);
    }

    @Transactional
    public void deleteSensor(Long deviceId) {
        sensorRepository.deleteById(deviceId);
    }
}