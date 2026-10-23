package com.smarthome.smart_home_backend.repository;

import com.smarthome.smart_home_backend.entity.NotificationPreference;
import com.smarthome.smart_home_backend.entity.NotificationPreferenceId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationPreferenceRepository
        extends JpaRepository<
        NotificationPreference,
        NotificationPreferenceId> {

    List<NotificationPreference>
    findByIdUserId(Long userId);

    List<NotificationPreference>
    findByIdDeviceId(Long deviceId);

    List<NotificationPreference>
    findByIdCategoryId(Long categoryId);
}