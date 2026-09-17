package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "CAMERAS")
public class Camera {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "resolution", length = 30)
    private String resolution;

    @Column(name = "storage_type", length = 30)
    private String storageType;

    @Column(name = "night_vision", length = 10)
    private String nightVision;

    public Camera() {
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getNightVision() {
        return nightVision;
    }

    public void setNightVision(String nightVision) {
        this.nightVision = nightVision;
    }
}