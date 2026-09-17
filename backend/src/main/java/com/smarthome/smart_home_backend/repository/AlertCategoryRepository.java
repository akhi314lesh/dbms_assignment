package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.AlertCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertCategoryRepository
        extends JpaRepository<AlertCategory, Long> {
}