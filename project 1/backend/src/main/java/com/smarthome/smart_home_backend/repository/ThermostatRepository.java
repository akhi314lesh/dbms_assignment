package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Thermostat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ThermostatRepository
        extends JpaRepository<Thermostat, Long> {

    @Query("SELECT t FROM Thermostat t WHERE t.device.room.home.homeId IN :homeIds")
    List<Thermostat> findByHomeIds(@Param("homeIds") Collection<Long> homeIds);
}