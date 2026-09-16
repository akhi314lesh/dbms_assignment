package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "SMART_LIGHTS")
public class SmartLight {

    @Id
    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "brightness")
    private Double brightness;

    @Column(name = "color_support", length = 20)
    private String colorSupport;

    public SmartLight() {
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Double getBrightness() {
        return brightness;
    }

    public void setBrightness(Double brightness) {
        this.brightness = brightness;
    }

    public String getColorSupport() {
        return colorSupport;
    }

    public void setColorSupport(String colorSupport) {
        this.colorSupport = colorSupport;
    }
}