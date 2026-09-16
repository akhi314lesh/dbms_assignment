package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class SensorReadingId implements Serializable {

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "reading_id")
    private Long readingId;

    public SensorReadingId() {
    }

    public SensorReadingId(Long deviceId, Long readingId) {
        this.deviceId = deviceId;
        this.readingId = readingId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getReadingId() {
        return readingId;
    }

    public void setReadingId(Long readingId) {
        this.readingId = readingId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof SensorReadingId)) return false;

        SensorReadingId that = (SensorReadingId) o;

        return Objects.equals(deviceId, that.deviceId)
                && Objects.equals(readingId, that.readingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, readingId);
    }
}