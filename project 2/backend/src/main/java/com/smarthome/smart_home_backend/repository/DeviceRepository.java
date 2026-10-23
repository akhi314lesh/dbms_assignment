package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByRoomRoomId(Long roomId);

    List<Device> findByDeviceSubtype(String deviceSubtype);

    List<Device> findByStatus(String status);

    List<Device> findByRoomHomeHomeIdIn(Collection<Long> homeIds);

    List<Device> findByRoomHomeHomeIdInAndDeviceSubtype(Collection<Long> homeIds, String deviceSubtype);

    List<Device> findByRoomHomeHomeIdInAndStatus(Collection<Long> homeIds, String status);

    List<Device> findByRoomRoomIdAndRoomHomeHomeIdIn(Long roomId, Collection<Long> homeIds);

    @Query("SELECT d.room.home.homeId FROM Device d WHERE d.deviceId = :deviceId")
    Optional<Long> findHomeIdByDeviceId(@Param("deviceId") Long deviceId);
}