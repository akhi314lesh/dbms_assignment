package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.MotionSensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MotionSensorRepository
        extends JpaRepository<MotionSensor, Long> {
}