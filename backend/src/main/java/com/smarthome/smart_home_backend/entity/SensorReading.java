package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "SENSOR_READINGS")
public class SensorReading {

    @EmbeddedId
    private SensorReadingId id;

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