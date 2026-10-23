package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.HomeAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-access")
public class HomeAccessController {

    private final HomeAccessService homeAccessService;
    private final HomeAuthorizationService authService;

    public HomeAccessController(HomeAccessService homeAccessService, HomeAuthorizationService authService) {
        this.homeAccessService = homeAccessService;
        this.authService = authService;
    }

    @GetMapping("/user/{userId}")
    public List<HomeAccess> getAccessByUser(@PathVariable Long userId) {
        authService.assertCanAccessUserProfile(userId);
        return homeAccessService.getAccessByUserId(userId);
    }

    @GetMapping("/home/{homeId}")
    public List<HomeAccess> getAccessByHome(@PathVariable Long homeId) {
        authService.assertCanAccessHome(homeId);
        return homeAccessService.getAccessByHomeId(homeId);
    }

    @GetMapping("/{userId}/{homeId}")
    public ResponseEntity<HomeAccess> getAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId) {

        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !caller.getUserId().equals(userId) && !authService.canAccessHome(caller, homeId)) {
            throw new AccessDeniedException("Access is denied.");
        }

        return homeAccessService.getAccess(userId, homeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/{homeId}")
    public ResponseEntity<HomeAccess> createAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId,
            @RequestBody HomeAccess access) {

        authService.assertCanManageHome(homeId);

        try {
            HomeAccess savedAccess = homeAccessService.createAccess(userId, homeId, access);
            return ResponseEntity.ok(savedAccess);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/{homeId}")
    public ResponseEntity<Void> deleteAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId) {

        User caller = authService.requireCurrentUser();
        if (!authService.isAdmin(caller) && !authService.canManageHome(caller, homeId) && !caller.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access is denied. Cannot revoke access for this home.");
        }

        if (homeAccessService.getAccess(userId, homeId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        homeAccessService.deleteAccess(userId, homeId);
        return ResponseEntity.noContent().build();
    }
}