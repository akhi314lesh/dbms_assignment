package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.Thermostat;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.ThermostatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ThermostatService {

    private final ThermostatRepository thermostatRepository;
    private final DeviceRepository deviceRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public ThermostatService(
            ThermostatRepository thermostatRepository,
            DeviceRepository deviceRepository,
            jakarta.persistence.EntityManager entityManager) {

        this.thermostatRepository = thermostatRepository;
        this.deviceRepository = deviceRepository;
        this.entityManager = entityManager;
    }

    public List<Thermostat> getAllThermostats() {
        return thermostatRepository.findAll();
    }

    public List<Thermostat> getThermostatsByHomeIds(java.util.Collection<Long> homeIds) {
        if (homeIds == null || homeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return thermostatRepository.findByHomeIds(homeIds);
    }

    public Optional<Thermostat> getThermostatById(Long deviceId) {
        return thermostatRepository.findById(deviceId);
    }

    @Transactional
    public Thermostat createThermostat(
            Long deviceId,
            Thermostat thermostat) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!"THERMOSTAT".equalsIgnoreCase(device.getDeviceSubtype())) {
            throw new RuntimeException(
                    "Device subtype is not THERMOSTAT"
            );
        }

        thermostat.setDevice(device);
        thermostat.setDeviceId(deviceId);

        entityManager.persist(thermostat);
        return thermostat;
    }

    @Transactional
    public Thermostat updateThermostat(
            Long deviceId,
            Thermostat thermostatDetails) {

        Thermostat existingThermostat =
                thermostatRepository.findById(deviceId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Thermostat not found"
                                )
                        );

        existingThermostat.setTargetTemperature(
                thermostatDetails.getTargetTemperature()
        );

        existingThermostat.setMode(
                thermostatDetails.getMode()
        );

        return thermostatRepository.save(existingThermostat);
    }

    @Transactional
    public void deleteThermostat(Long deviceId) {
        thermostatRepository.deleteById(deviceId);
    }
}