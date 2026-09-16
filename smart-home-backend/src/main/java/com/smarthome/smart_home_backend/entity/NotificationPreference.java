package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "NOTIFICATION_PREFERENCES")
public class NotificationPreference {

    @EmbeddedId
    private NotificationPreferenceId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @MapsId("deviceId")
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne
    @MapsId("categoryId")
    @JoinColumn(name = "category_id", nullable = false)
    private AlertCategory category;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "enabled", nullable = false)
    private Integer enabled;

    public NotificationPreference() {
    }

    public NotificationPreferenceId getId() {
        return id;
    }

    public void setId(NotificationPreferenceId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public AlertCategory getCategory() {
        return category;
    }

    public void setCategory(AlertCategory category) {
        this.category = category;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}