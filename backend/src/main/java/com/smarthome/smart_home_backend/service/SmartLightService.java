package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.SmartLight;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.SmartLightRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SmartLightService {

    private final SmartLightRepository smartLightRepository;
    private final DeviceRepository deviceRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public SmartLightService(
            SmartLightRepository smartLightRepository,
            DeviceRepository deviceRepository,
            jakarta.persistence.EntityManager entityManager) {

        this.smartLightRepository = smartLightRepository;
        this.deviceRepository = deviceRepository;
        this.entityManager = entityManager;
    }

    public List<SmartLight> getAllSmartLights() {
        return smartLightRepository.findAll();
    }

    public List<SmartLight> getSmartLightsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return smartLightRepository.findByHomeIds(homeIds);
    }

    public Optional<SmartLight> getSmartLightById(Long deviceId) {
        return smartLightRepository.findById(deviceId);
    }

    @Transactional
    public SmartLight createSmartLight(
            Long deviceId,
            SmartLight smartLight) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!"SMART_LIGHT".equalsIgnoreCase(device.getDeviceSubtype())) {
            throw new RuntimeException(
                    "Device subtype is not SMART_LIGHT"
            );
        }

        smartLight.setDevice(device);
        smartLight.setDeviceId(deviceId);

        entityManager.persist(smartLight);
        return smartLight;
    }

    @Transactional
    public SmartLight updateSmartLight(
            Long deviceId,
            SmartLight smartLightDetails) {

        SmartLight existingSmartLight =
                smartLightRepository.findById(deviceId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Smart light not found"
                                )
                        );

        existingSmartLight.setBrightness(
                smartLightDetails.getBrightness()
        );

        existingSmartLight.setColorSupport(
                smartLightDetails.getColorSupport()
        );

        return smartLightRepository.save(existingSmartLight);
    }

    @Transactional
    public void deleteSmartLight(Long deviceId) {
        smartLightRepository.deleteById(deviceId);
    }
}