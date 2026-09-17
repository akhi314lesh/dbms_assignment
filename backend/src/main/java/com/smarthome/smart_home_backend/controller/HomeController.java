package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.entity.Home;
import com.smarthome.smart_home_backend.service.HomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homes")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public List<Home> getAllHomes() {
        return homeService.getAllHomes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Home> getHomeById(
            @PathVariable Long id) {

        return homeService.getHomeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Home> createHome(
            @RequestBody Home home) {

        Home savedHome = homeService.createHome(home);

        return ResponseEntity.ok(savedHome);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Home> updateHome(
            @PathVariable Long id,
            @RequestBody Home homeDetails) {

        try {
            Home updatedHome =
                    homeService.updateHome(id, homeDetails);

            return ResponseEntity.ok(updatedHome);

        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHome(
            @PathVariable Long id) {

        try {
            homeService.deleteHome(id);

            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}