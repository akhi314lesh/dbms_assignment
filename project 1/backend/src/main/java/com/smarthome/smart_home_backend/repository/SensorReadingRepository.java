package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.SensorReading;
import com.smarthome.smart_home_backend.entity.SensorReadingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SensorReadingRepository
        extends JpaRepository<SensorReading, SensorReadingId> {

    List<SensorReading> findByIdDeviceId(Long deviceId);

    List<SensorReading> findByDeviceRoomHomeHomeIdIn(Collection<Long> homeIds);

    List<SensorReading> findByIdDeviceIdAndDeviceRoomHomeHomeIdIn(Long deviceId, Collection<Long> homeIds);
}