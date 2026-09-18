package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.entity.SensorReadingId;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.SensorReadingRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SensorReadingService {

    private final SensorReadingRepository readingRepository;
    private final DeviceRepository deviceRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    public SensorReadingService(
            SensorReadingRepository readingRepository,
            DeviceRepository deviceRepository,
            org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {

        this.readingRepository = readingRepository;
        this.deviceRepository = deviceRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SensorReading> getAllReadings() {
        return readingRepository.findAll();
    }

    public List<SensorReading> getReadingsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return readingRepository.findByDeviceRoomHomeHomeIdIn(homeIds);
    }

    public List<SensorReading> getReadingsByDeviceId(Long deviceId) {
        return readingRepository.findByIdDeviceId(deviceId);
    }

    public List<SensorReading> getReadingsByDeviceIdAndHomeIds(Long deviceId, java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return readingRepository.findByIdDeviceIdAndDeviceRoomHomeHomeIdIn(deviceId, homeIds);
    }

    public Optional<SensorReading> getReading(
            Long deviceId,
            Long readingId) {

        SensorReadingId id =
                new SensorReadingId(deviceId, readingId);

        return readingRepository.findById(id);
    }

    @Transactional
    public SensorReading createReading(
            Long deviceId,
            SensorReading reading) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        if (reading.getReadingId() == null) {
            Long readingId = jdbcTemplate.execute((java.sql.Connection conn) -> {
                try (java.sql.CallableStatement cs = conn.prepareCall("BEGIN sp_record_sensor_reading(?, ?, ?); END;")) {
                    cs.setLong(1, deviceId);
                    cs.setDouble(2, reading.getValue() != null ? reading.getValue() : 0.0);
                    cs.registerOutParameter(3, java.sql.Types.NUMERIC);
                    cs.execute();
                    return cs.getLong(3);
                }
            });
            return readingRepository.findById(new SensorReadingId(deviceId, readingId))
                    .orElseThrow(() -> new RuntimeException("Recorded reading not found"));
        }

        reading.setDevice(device);
        reading.setDeviceId(deviceId);

        return readingRepository.save(reading);
    }

    @Transactional
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

    @Transactional
    public void deleteReading(
            Long deviceId,
            Long readingId) {

        SensorReadingId id =
                new SensorReadingId(deviceId, readingId);

        readingRepository.deleteById(id);
    }
}