package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Thermostat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThermostatRepository
        extends JpaRepository<Thermostat, Long> {
}