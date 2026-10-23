package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.dto.AuthUserDto;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Controller for returning current user profile in dev/team mode.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> getCurrentUser(Authentication authentication) {
        // Find existing user in database or return default dev admin
        Optional<User> firstUser = userRepository.findById(1L);
        if (firstUser.isPresent()) {
            User user = firstUser.get();
            return ResponseEntity.ok(new AuthUserDto(
                    user.getUserId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    "dev-local-user-1"
            ));
        }

        // Fallback default dev admin
        return ResponseEntity.ok(new AuthUserDto(
                1L,
                "DA3 Teammate",
                "teammate@smarthome.local",
                "ADMIN",
                "dev-local-user-1"
        ));
    }
}
