package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.TemperatureSensor;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.TemperatureSensorRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TemperatureSensorService {

    private final TemperatureSensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public TemperatureSensorService(
            TemperatureSensorRepository sensorRepository,
            DeviceRepository deviceRepository,
            jakarta.persistence.EntityManager entityManager) {

        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
        this.entityManager = entityManager;
    }

    public List<TemperatureSensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    public List<TemperatureSensor> getSensorsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return sensorRepository.findByHomeIds(homeIds);
    }

    public Optional<TemperatureSensor> getSensorById(Long deviceId) {
        return sensorRepository.findById(deviceId);
    }

    @Transactional
    public TemperatureSensor createSensor(
            Long deviceId,
            TemperatureSensor sensor) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!"TEMPERATURE_SENSOR".equalsIgnoreCase(
                device.getDeviceSubtype())) {

            throw new RuntimeException(
                    "Device subtype is not TEMPERATURE_SENSOR"
            );
        }

        sensor.setDevice(device);
        sensor.setDeviceId(deviceId);

        entityManager.persist(sensor);
        return sensor;
    }

    @Transactional
    public TemperatureSensor updateSensor(
            Long deviceId,
            TemperatureSensor details) {

        TemperatureSensor existing =
                sensorRepository.findById(deviceId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Temperature sensor not found"
                                )
                        );

        existing.setUnit(details.getUnit());
        existing.setMinRange(details.getMinRange());
        existing.setMaxRange(details.getMaxRange());

        return sensorRepository.save(existing);
    }

    @Transactional
    public void deleteSensor(Long deviceId) {
        sensorRepository.deleteById(deviceId);
    }
}