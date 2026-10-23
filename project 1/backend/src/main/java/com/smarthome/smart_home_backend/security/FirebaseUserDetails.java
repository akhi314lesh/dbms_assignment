package com.smarthome.smart_home_backend.security;

import com.smarthome.smart_home_backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FirebaseUserDetails implements UserDetails {

    private final String firebaseUid;
    private final String email;
    private final User user;
    private final Collection<? extends GrantedAuthority> authorities;

    public FirebaseUserDetails(String firebaseUid, String email, User user) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.user = user;
        String role = (user != null && user.getRole() != null) ? user.getRole() : "USER";
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getEmail() {
        return email;
    }

    public User getUser() {
        return user;
    }

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public String getRole() {
        return user != null ? user.getRole() : "USER";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : firebaseUid;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
