package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.entity.Thermostat;
import com.smarthome.smart_home_backend.repository.DeviceRepository;
import com.smarthome.smart_home_backend.repository.ThermostatRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ThermostatService {

    private final ThermostatRepository thermostatRepository;
    private final DeviceRepository deviceRepository;

    public ThermostatService(
            ThermostatRepository thermostatRepository,
            DeviceRepository deviceRepository) {

        this.thermostatRepository = thermostatRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<Thermostat> getAllThermostats() {
        return thermostatRepository.findAll();
    }

    public Optional<Thermostat> getThermostatById(Long deviceId) {
        return thermostatRepository.findById(deviceId);
    }

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

        thermostat.setDeviceId(deviceId);

        return thermostatRepository.save(thermostat);
    }

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

    public void deleteThermostat(Long deviceId) {
        thermostatRepository.deleteById(deviceId);
    }
}