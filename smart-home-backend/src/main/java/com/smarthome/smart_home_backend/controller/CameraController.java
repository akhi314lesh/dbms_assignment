package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Camera;
import com.smarthome.smart_home_backend.service.CameraService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraService cameraService;

    public CameraController(CameraService cameraService) {
        this.cameraService = cameraService;
    }

    @GetMapping
    public List<Camera> getAllCameras() {
        return cameraService.getAllCameras();
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<Camera> getCameraById(
            @PathVariable Long deviceId) {

        return cameraService.getCameraById(deviceId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/device/{deviceId}")
    public ResponseEntity<Camera> createCamera(
            @PathVariable Long deviceId,
            @RequestBody Camera camera) {

        try {
            return ResponseEntity.ok(
                    cameraService.createCamera(deviceId, camera)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<Camera> updateCamera(
            @PathVariable Long deviceId,
            @RequestBody Camera details) {

        try {
            return ResponseEntity.ok(
                    cameraService.updateCamera(deviceId, details)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> deleteCamera(
            @PathVariable Long deviceId) {

        try {
            cameraService.deleteCamera(deviceId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}