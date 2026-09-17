package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository
        extends JpaRepository<Alert, Long> {

    List<Alert> findByDeviceDeviceId(Long deviceId);

    List<Alert> findByStatus(String status);

    List<Alert> findByCategoryCategoryId(Long categoryId);
}