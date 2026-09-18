package com.smarthome.smart_home_backend.dto;

public class AuthUserDto {

    private Long userId;
    private String name;
    private String email;
    private String role;
    private String firebaseUid;

    public AuthUserDto() {
    }

    public AuthUserDto(Long userId, String name, String email, String role, String firebaseUid) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.firebaseUid = firebaseUid;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}
