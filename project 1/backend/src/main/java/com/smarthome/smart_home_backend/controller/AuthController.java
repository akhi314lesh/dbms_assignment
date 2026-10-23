package com.smarthome.smart_home_backend.controller;

import com.smarthome.smart_home_backend.dto.AuthUserDto;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.security.FirebaseUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<AuthUserDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof FirebaseUserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userDetails.getUser();
        AuthUserDto dto = new AuthUserDto(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                userDetails.getFirebaseUid()
        );

        return ResponseEntity.ok(dto);
    }
}
