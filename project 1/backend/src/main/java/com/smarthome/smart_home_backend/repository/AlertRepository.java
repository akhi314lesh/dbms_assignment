package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AlertRepository
        extends JpaRepository<Alert, Long> {

    List<Alert> findByDeviceDeviceId(Long deviceId);

    List<Alert> findByStatus(String status);

    List<Alert> findByCategoryCategoryId(Long categoryId);

    List<Alert> findByDeviceRoomHomeHomeIdIn(Collection<Long> homeIds);

    List<Alert> findByDeviceDeviceIdAndDeviceRoomHomeHomeIdIn(Long deviceId, Collection<Long> homeIds);

    List<Alert> findByStatusAndDeviceRoomHomeHomeIdIn(String status, Collection<Long> homeIds);

    List<Alert> findByCategoryCategoryIdAndDeviceRoomHomeHomeIdIn(Long categoryId, Collection<Long> homeIds);

    @Query("SELECT a.device.room.home.homeId FROM Alert a WHERE a.alertId = :alertId")
    Optional<Long> findHomeIdByAlertId(@Param("alertId") Long alertId);
}