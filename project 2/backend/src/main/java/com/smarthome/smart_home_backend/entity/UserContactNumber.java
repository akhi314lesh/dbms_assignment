package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "USER_CONTACT_NUMBERS")
public class UserContactNumber {

    @EmbeddedId
    private UserContactNumberId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "number_type", length = 20)
    private String numberType;

    public UserContactNumber() {
    }

    public UserContactNumberId getId() {
        return id;
    }

    public void setId(UserContactNumberId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getContactNumber() {
        if (id == null) {
            return null;
        }

        return id.getContactNumber();
    }

    public void setContactNumber(String contactNumber) {
        if (this.id == null) {
            this.id = new UserContactNumberId();
        }

        this.id.setContactNumber(contactNumber);
    }

    public String getNumberType() {
        return numberType;
    }

    public void setNumberType(String numberType) {
        this.numberType = numberType;
    }
}