package com.smarthome.smart_home_backend.service;

import com.smarthome.smart_home_backend.entity.Camera;
import com.smarthome.smart_home_backend.entity.Device;
import com.smarthome.smart_home_backend.repository.CameraRepository;
import com.smarthome.smart_home_backend.repository.DeviceRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CameraService {

    private final CameraRepository cameraRepository;
    private final DeviceRepository deviceRepository;

    public CameraService(
            CameraRepository cameraRepository,
            DeviceRepository deviceRepository) {

        this.cameraRepository = cameraRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<Camera> getAllCameras() {
        return cameraRepository.findAll();
    }

    public Optional<Camera> getCameraById(Long deviceId) {
        return cameraRepository.findById(deviceId);
    }

    public Camera createCamera(
            Long deviceId,
            Camera camera) {

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (!"CAMERA".equalsIgnoreCase(
                device.getDeviceSubtype())) {

            throw new RuntimeException(
                    "Device subtype is not CAMERA"
            );
        }

        camera.setDeviceId(deviceId);

        return cameraRepository.save(camera);
    }

    public Camera updateCamera(
            Long deviceId,
            Camera details) {

        Camera existing =
                cameraRepository.findById(deviceId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Camera not found"
                                )
                        );

        existing.setResolution(details.getResolution());
        existing.setStorageType(details.getStorageType());
        existing.setNightVision(details.getNightVision());

        return cameraRepository.save(existing);
    }

    public void deleteCamera(Long deviceId) {
        cameraRepository.deleteById(deviceId);
    }
}