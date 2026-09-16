package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.TemperatureSensor;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.TemperatureSensorRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TemperatureSensorService {

    private final TemperatureSensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;

    public TemperatureSensorService(
            TemperatureSensorRepository sensorRepository,
            DeviceRepository deviceRepository) {

        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<TemperatureSensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    public Optional<TemperatureSensor> getSensorById(Long deviceId) {
        return sensorRepository.findById(deviceId);
    }

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

        sensor.setDeviceId(deviceId);

        return sensorRepository.save(sensor);
    }

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

    public void deleteSensor(Long deviceId) {
        sensorRepository.deleteById(deviceId);
    }
}