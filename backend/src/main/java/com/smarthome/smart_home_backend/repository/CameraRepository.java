package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Camera;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraRepository
        extends JpaRepository<Camera, Long> {
}