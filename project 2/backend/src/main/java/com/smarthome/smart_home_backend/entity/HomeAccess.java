package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "HOME_ACCESS")
public class HomeAccess {

    @EmbeddedId
    private HomeAccessId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @MapsId("homeId")
    @JoinColumn(name = "home_id", nullable = false)
    private Home home;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "date_granted", nullable = false)
    private LocalDate dateGranted;

    public HomeAccess() {
    }

    public HomeAccessId getId() {
        return id;
    }

    public void setId(HomeAccessId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Home getHome() {
        return home;
    }

    public void setHome(Home home) {
        this.home = home;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getDateGranted() {
        return dateGranted;
    }

    public void setDateGranted(LocalDate dateGranted) {
        this.dateGranted = dateGranted;
    }
}