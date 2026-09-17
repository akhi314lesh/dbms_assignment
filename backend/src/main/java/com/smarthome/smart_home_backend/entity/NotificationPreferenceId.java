package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class NotificationPreferenceId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "category_id")
    private Long categoryId;

    public NotificationPreferenceId() {
    }

    public NotificationPreferenceId(
            Long userId,
            Long deviceId,
            Long categoryId) {

        this.userId = userId;
        this.deviceId = deviceId;
        this.categoryId = categoryId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof NotificationPreferenceId)) {
            return false;
        }

        NotificationPreferenceId that =
                (NotificationPreferenceId) o;

        return Objects.equals(userId, that.userId)
                && Objects.equals(deviceId, that.deviceId)
                && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                userId,
                deviceId,
                categoryId
        );
    }
}