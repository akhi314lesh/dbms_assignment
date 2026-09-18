package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.MotionSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MotionSensorRepository
        extends JpaRepository<MotionSensor, Long> {

    @Query("SELECT m FROM MotionSensor m WHERE m.device.room.home.homeId IN :homeIds")
    List<MotionSensor> findByHomeIds(@Param("homeIds") Collection<Long> homeIds);
}