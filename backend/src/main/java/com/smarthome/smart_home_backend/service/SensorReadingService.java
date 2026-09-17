package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.entity.SensorReadingId;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.SensorReadingRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SensorReadingService {

    private final SensorReadingRepository readingRepository;
    private final DeviceRepository deviceRepository;

    public SensorReadingService(
            SensorReadingRepository readingRepository,
            DeviceRepository deviceRepository) {

        this.readingRepository = readingRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<SensorReading> getAllReadings() {
        return readingRepository.findAll();
    }

    public List<SensorReading> getReadingsByDeviceId(Long deviceId) {
        return readingRepository.findByIdDeviceId(deviceId);
    }

    public Optional<SensorReading> getReading(
            Long deviceId,
            Long readingId) {

        SensorReadingId id =
                new SensorReadingId(deviceId, readingId);

        return readingRepository.findById(id);
    }

    public SensorReading createReading(
            Long deviceId,
            SensorReading reading) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        if (reading.getReadingId() == null) {
            throw new RuntimeException("Reading ID is required");
        }

        reading.setDeviceId(deviceId);

        return readingRepository.save(reading);
    }

    public SensorReading updateReading(
            Long deviceId,
            Long readingId,
            SensorReading details) {

        SensorReadingId id =
                new SensorReadingId(deviceId, readingId);

        SensorReading existing =
                readingRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Sensor reading not found"
                                )
                        );

        existing.setValue(details.getValue());
        existing.setReadingTime(details.getReadingTime());

        return readingRepository.save(existing);
    }

    public void deleteReading(
            Long deviceId,
            Long readingId) {

        SensorReadingId id =
                new SensorReadingId(deviceId, readingId);

        readingRepository.deleteById(id);
    }
}