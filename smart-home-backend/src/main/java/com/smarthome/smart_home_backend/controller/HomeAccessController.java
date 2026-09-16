package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.service.HomeAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-access")
public class HomeAccessController {

    private final HomeAccessService homeAccessService;

    public HomeAccessController(HomeAccessService homeAccessService) {
        this.homeAccessService = homeAccessService;
    }

    @GetMapping("/user/{userId}")
    public List<HomeAccess> getAccessByUser(
            @PathVariable Long userId) {

        return homeAccessService.getAccessByUserId(userId);
    }

    @GetMapping("/home/{homeId}")
    public List<HomeAccess> getAccessByHome(
            @PathVariable Long homeId) {

        return homeAccessService.getAccessByHomeId(homeId);
    }

    @GetMapping("/{userId}/{homeId}")
    public ResponseEntity<HomeAccess> getAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId) {

        return homeAccessService.getAccess(userId, homeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{userId}/{homeId}")
    public ResponseEntity<HomeAccess> createAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId,
            @RequestBody HomeAccess access) {

        try {
            HomeAccess savedAccess =
                    homeAccessService.createAccess(
                            userId, homeId, access);

            return ResponseEntity.ok(savedAccess);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{userId}/{homeId}")
    public ResponseEntity<Void> deleteAccess(
            @PathVariable Long userId,
            @PathVariable Long homeId) {

        if (homeAccessService.getAccess(userId, homeId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        homeAccessService.deleteAccess(userId, homeId);

        return ResponseEntity.noContent().build();
    }
}