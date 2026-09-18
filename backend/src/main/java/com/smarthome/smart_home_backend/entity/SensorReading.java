package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "SENSOR_READINGS")
public class SensorReading {

    @EmbeddedId
    private SensorReadingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("deviceId")
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "reading_time", nullable = false)
    private LocalDateTime readingTime;

    public SensorReading() {
    }

    public SensorReadingId getId() {
        return id;
    }

    public void setId(SensorReadingId id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
        if (device != null) {
            if (this.id == null) {
                this.id = new SensorReadingId();
            }
            this.id.setDeviceId(device.getDeviceId());
        }
    }

    public Long getDeviceId() {
        return id != null ? id.getDeviceId() : null;
    }

    public void setDeviceId(Long deviceId) {
        if (this.id == null) {
            this.id = new SensorReadingId();
        }

        this.id.setDeviceId(deviceId);
    }

    public Long getReadingId() {
        return id != null ? id.getReadingId() : null;
    }

    public void setReadingId(Long readingId) {
        if (this.id == null) {
            this.id = new SensorReadingId();
        }

        this.id.setReadingId(readingId);
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public LocalDateTime getReadingTime() {
        return readingTime;
    }

    public void setReadingTime(LocalDateTime readingTime) {
        this.readingTime = readingTime;
    }
}