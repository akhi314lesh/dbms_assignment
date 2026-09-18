package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final HomeAuthorizationService authService;

    public UserController(UserService userService, HomeAuthorizationService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller)) {
            throw new AccessDeniedException("Access is denied. Admin role required.");
        }
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !caller.getUserId().equals(id)) {
            throw new AccessDeniedException("Access is denied. Cannot access another user's profile.");
        }
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller)) {
            throw new AccessDeniedException("Access is denied. Admin role required.");
        }
        User savedUser = userService.createUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails) {

        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !caller.getUserId().equals(id)) {
            throw new AccessDeniedException("Access is denied. Cannot modify another user's profile.");
        }

        if (!authService.isAdmin(caller)) {
            userDetails.setRole(caller.getRole());
            userDetails.setFirebaseUid(caller.getFirebaseUid());
        }

        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller)) {
            throw new AccessDeniedException("Access is denied. Admin role required.");
        }
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !caller.getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Access is denied. Cannot access another user's profile.");
        }
        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}