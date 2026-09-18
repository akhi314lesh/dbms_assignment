package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.HomeAuthorizationService;
import com.smarthome.smart_home_backend.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homes")
public class HomeController {

    private final HomeService homeService;
    private final HomeAuthorizationService authService;

    public HomeController(HomeService homeService, HomeAuthorizationService authService) {
        this.homeService = homeService;
        this.authService = authService;
    }

    @GetMapping
    public List<Home> getAllHomes() {
        User user = authService.requireCurrentUser();
        if (authService.isAdmin(user)) {
            return homeService.getAllHomes();
        }
        return homeService.getHomesByIds(authService.getAccessibleHomeIds(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Home> getHomeById(@PathVariable Long id) {
        authService.assertCanAccessHome(id);
        return homeService.getHomeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/health-report")
    public ResponseEntity<java.util.Map<String, String>> getHealthReport(@PathVariable Long id) {
        authService.assertCanAccessHome(id);
        String report = homeService.getHealthReport(id);
        return ResponseEntity.ok(java.util.Map.of("reportText", report));
    }

    @PostMapping
    public ResponseEntity<Home> createHome(@RequestBody Home home) {
        User user = authService.requireCurrentUser();
        Home savedHome = homeService.createHome(home, user);
        return ResponseEntity.ok(savedHome);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Home> updateHome(
            @PathVariable Long id,
            @RequestBody Home homeDetails) {

        authService.assertCanManageHome(id);
        try {
            Home updatedHome = homeService.updateHome(id, homeDetails);
            return ResponseEntity.ok(updatedHome);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHome(@PathVariable Long id) {
        authService.assertCanManageHome(id);
        try {
            homeService.deleteHome(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}