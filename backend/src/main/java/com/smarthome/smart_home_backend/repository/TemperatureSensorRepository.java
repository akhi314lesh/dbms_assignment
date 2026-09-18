package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.TemperatureSensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TemperatureSensorRepository
        extends JpaRepository<TemperatureSensor, Long> {

    @Query("SELECT t FROM TemperatureSensor t WHERE t.device.room.home.homeId IN :homeIds")
    List<TemperatureSensor> findByHomeIds(@Param("homeIds") Collection<Long> homeIds);
}