package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MOTION_SENSORS")
public class MotionSensor {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "sensitivity_level", length = 30)
    private String sensitivityLevel;

    @Column(name = "detection_range")
    private Double detectionRange;

    public MotionSensor() {
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(String sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public Double getDetectionRange() {
        return detectionRange;
    }

    public void setDetectionRange(Double detectionRange) {
        this.detectionRange = detectionRange;
    }
}