package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.RuleAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleActionRepository
        extends JpaRepository<RuleAction, Long> {

    List<RuleAction> findByRuleRuleId(Long ruleId);

    List<RuleAction> findByTargetDeviceDeviceId(Long deviceId);
}