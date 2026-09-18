package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.RuleAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RuleActionRepository
        extends JpaRepository<RuleAction, Long> {

    List<RuleAction> findByRuleRuleId(Long ruleId);

    List<RuleAction> findByTargetDeviceDeviceId(Long deviceId);

    List<RuleAction> findByTargetDeviceRoomHomeHomeIdIn(Collection<Long> homeIds);

    @Query("SELECT ra.targetDevice.room.home.homeId FROM RuleAction ra WHERE ra.actionId = :actionId")
    Optional<Long> findHomeIdByActionId(@Param("actionId") Long actionId);
}