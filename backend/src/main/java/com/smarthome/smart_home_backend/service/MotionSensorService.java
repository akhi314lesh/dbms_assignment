package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.MotionSensor;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.MotionSensorRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MotionSensorService {

    private final MotionSensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;

    public MotionSensorService(
            MotionSensorRepository sensorRepository,
            DeviceRepository deviceRepository) {

        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<MotionSensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    public Optional<MotionSensor> getSensorById(Long deviceId) {
        return sensorRepository.findById(deviceId);
    }

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

        sensor.setDeviceId(deviceId);

        return sensorRepository.save(sensor);
    }

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

    public void deleteSensor(Long deviceId) {
        sensorRepository.deleteById(deviceId);
    }
}