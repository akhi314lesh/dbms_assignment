package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutomationRuleRepository
        extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByCreatedByUserUserId(Long userId);

    List<AutomationRule> findByConditionDeviceDeviceId(Long deviceId);
}