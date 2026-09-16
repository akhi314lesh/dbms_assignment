package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.TemperatureSensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemperatureSensorRepository
        extends JpaRepository<TemperatureSensor, Long> {
}