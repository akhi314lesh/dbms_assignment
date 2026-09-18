package com.smarthome.smart_home_backend.service;

import com.google.firebase.auth.FirebaseToken;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository userRepository;
    private final String adminGoogleEmail;

    public UserProvisioningService(
            UserRepository userRepository,
            org.springframework.core.env.Environment env) {

        this.userRepository = userRepository;
        String email = env.getProperty("ADMIN_GOOGLE_EMAIL");
        if (email == null || email.isBlank()) {
            email = env.getProperty("admin.google.email", "");
        }
        this.adminGoogleEmail = email.trim().toLowerCase();
        if (!this.adminGoogleEmail.isEmpty()) {
            log.info("Administrator email configured via ADMIN_GOOGLE_EMAIL (masked: {}***)",
                    this.adminGoogleEmail.length() > 3 ? this.adminGoogleEmail.substring(0, 3) : "***");
        } else {
            log.info("ADMIN_GOOGLE_EMAIL environment variable is not configured.");
        }
    }

    @Transactional
    public User resolveOrProvisionUser(FirebaseToken decodedToken) {
        String uid = decodedToken.getUid();
        String rawEmail = decodedToken.getEmail();
        String email = (rawEmail != null) ? rawEmail.trim().toLowerCase() : null;

        String name = decodedToken.getName();
        if (name == null || name.isBlank()) {
            name = (email != null) ? email.split("@")[0] : "Google User";
        }

        boolean isAdmin = (email != null && !adminGoogleEmail.isEmpty() && email.equals(adminGoogleEmail));

        // 1. Primary lookup by stable firebase_uid
        Optional<User> userByUid = userRepository.findByFirebaseUid(uid);
        if (userByUid.isPresent()) {
            User user = userByUid.get();
            if (isAdmin && !"ADMIN".equals(user.getRole())) {
                user.setRole("ADMIN");
                user = userRepository.save(user);
                log.info("Promoted user ID {} to ADMIN based on configured ADMIN_GOOGLE_EMAIL", user.getUserId());
            }
            return user;
        }

        // 2. Secondary fallback lookup by verified email to link pre-existing account
        if (email != null) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                user.setFirebaseUid(uid);
                if (isAdmin) {
                    user.setRole("ADMIN");
                }
                user = userRepository.save(user);
                log.info("Linked Firebase UID to existing Oracle user ID {} (role: {})", user.getUserId(), user.getRole());
                return user;
            }
        }

        // 3. Auto-provision new Google self-registered account
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email != null ? email : (uid + "@firebase.user"));
        newUser.setFirebaseUid(uid);
        newUser.setRole(isAdmin ? "ADMIN" : "USER");
        newUser.setPassword(null);

        User savedUser = userRepository.save(newUser);
        log.info("Self-provisioned new Oracle user ID {} (role: {}) for Firebase UID: {}",
                savedUser.getUserId(), savedUser.getRole(), uid);
        return savedUser;
    }
}
