package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AutomationRuleRepository
        extends JpaRepository<AutomationRule, Long> {

    List<AutomationRule> findByCreatedByUserUserId(Long userId);

    List<AutomationRule> findByConditionDeviceDeviceId(Long deviceId);

    List<AutomationRule> findByConditionDeviceRoomHomeHomeIdIn(Collection<Long> homeIds);

    List<AutomationRule> findByConditionDeviceRoomHomeHomeIdInOrCreatedByUserUserId(Collection<Long> homeIds, Long userId);

    @Query("SELECT r.conditionDevice.room.home.homeId FROM AutomationRule r WHERE r.ruleId = :ruleId")
    Optional<Long> findHomeIdByRuleId(@Param("ruleId") Long ruleId);
}