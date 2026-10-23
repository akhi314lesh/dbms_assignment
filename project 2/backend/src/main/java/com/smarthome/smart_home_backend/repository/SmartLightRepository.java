package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.SmartLight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SmartLightRepository
        extends JpaRepository<SmartLight, Long> {

    @Query("SELECT s FROM SmartLight s WHERE s.device.room.home.homeId IN :homeIds")
    List<SmartLight> findByHomeIds(@Param("homeIds") Collection<Long> homeIds);
}