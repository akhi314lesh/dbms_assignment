package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByRoomRoomId(Long roomId);

    List<Device> findByDeviceSubtype(String deviceSubtype);

    List<Device> findByStatus(String status);
}