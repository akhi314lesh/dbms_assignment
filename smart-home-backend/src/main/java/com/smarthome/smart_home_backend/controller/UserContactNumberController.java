package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.UserContactNumber;
import com.smarthome.smart_home_backend.service.UserContactNumberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/contact-numbers")
public class UserContactNumberController {

    private final UserContactNumberService service;

    public UserContactNumberController(
            UserContactNumberService service) {

        this.service = service;
    }

    @GetMapping
    public List<UserContactNumber> getContacts(
            @PathVariable Long userId) {

        return service.getContactsByUserId(userId);
    }

    @GetMapping("/{contactNumber}")
    public ResponseEntity<UserContactNumber> getContact(
            @PathVariable Long userId,
            @PathVariable String contactNumber) {

        return service.getContact(userId, contactNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserContactNumber> createContact(
            @PathVariable Long userId,
            @RequestBody UserContactNumber contact) {

        try {
            UserContactNumber saved =
                    service.createContact(userId, contact);

            return ResponseEntity.ok(saved);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{contactNumber}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable Long userId,
            @PathVariable String contactNumber) {

        if (service.getContact(userId, contactNumber).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        service.deleteContact(userId, contactNumber);

        return ResponseEntity.noContent().build();
    }
}