package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.*;
import com.smarthome.smart_home_backend.repository.*;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final AlertCategoryRepository categoryRepository;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            AlertCategoryRepository categoryRepository) {

        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<NotificationPreference> getAllPreferences() {
        return preferenceRepository.findAll();
    }

    public List<NotificationPreference> getByUser(Long userId) {
        return preferenceRepository.findByIdUserId(userId);
    }

    public List<NotificationPreference> getByDevice(Long deviceId) {
        return preferenceRepository.findByIdDeviceId(deviceId);
    }

    public List<NotificationPreference> getByCategory(
            Long categoryId) {

        return preferenceRepository.findByIdCategoryId(categoryId);
    }

    public Optional<NotificationPreference> getPreference(
            Long userId,
            Long deviceId,
            Long categoryId) {

        NotificationPreferenceId id =
                new NotificationPreferenceId(
                        userId,
                        deviceId,
                        categoryId
                );

        return preferenceRepository.findById(id);
    }

    public NotificationPreference createPreference(
            Long userId,
            Long deviceId,
            Long categoryId,
            NotificationPreference preference) {

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(
                        () -> new RuntimeException("Device not found")
                );

        AlertCategory category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Alert category not found"
                                )
                        );

        NotificationPreferenceId id =
                new NotificationPreferenceId(
                        userId,
                        deviceId,
                        categoryId
                );

        preference.setId(id);
        preference.setUser(user);
        preference.setDevice(device);
        preference.setCategory(category);

        return preferenceRepository.save(preference);
    }

    public NotificationPreference updatePreference(
            Long userId,
            Long deviceId,
            Long categoryId,
            NotificationPreference details) {

        NotificationPreferenceId id =
                new NotificationPreferenceId(
                        userId,
                        deviceId,
                        categoryId
                );

        NotificationPreference existing =
                preferenceRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Notification preference not found"
                                )
                        );

        existing.setChannel(details.getChannel());
        existing.setEnabled(details.getEnabled());

        return preferenceRepository.save(existing);
    }

    public void deletePreference(
            Long userId,
            Long deviceId,
            Long categoryId) {

        NotificationPreferenceId id =
                new NotificationPreferenceId(
                        userId,
                        deviceId,
                        categoryId
                );

        preferenceRepository.deleteById(id);
    }
}