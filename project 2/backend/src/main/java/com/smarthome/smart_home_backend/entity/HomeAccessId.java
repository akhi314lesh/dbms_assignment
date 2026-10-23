package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class HomeAccessId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "home_id")
    private Long homeId;

    public HomeAccessId() {
    }

    public HomeAccessId(Long userId, Long homeId) {
        this.userId = userId;
        this.homeId = homeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getHomeId() {
        return homeId;
    }

    public void setHomeId(Long homeId) {
        this.homeId = homeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof HomeAccessId)) {
            return false;
        }

        HomeAccessId that = (HomeAccessId) o;

        return Objects.equals(userId, that.userId)
                && Objects.equals(homeId, that.homeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, homeId);
    }
}