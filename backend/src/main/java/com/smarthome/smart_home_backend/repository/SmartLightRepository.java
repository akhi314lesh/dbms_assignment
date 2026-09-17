package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.SmartLight;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmartLightRepository
        extends JpaRepository<SmartLight, Long> {
}