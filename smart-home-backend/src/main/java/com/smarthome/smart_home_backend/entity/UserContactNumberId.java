package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserContactNumberId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "contact_number")
    private String contactNumber;

    public UserContactNumberId() {
    }

    public UserContactNumberId(Long userId, String contactNumber) {
        this.userId = userId;
        this.contactNumber = contactNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserContactNumberId)) {
            return false;
        }

        UserContactNumberId that = (UserContactNumberId) o;

        return Objects.equals(userId, that.userId)
                && Objects.equals(contactNumber, that.contactNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, contactNumber);
    }
}