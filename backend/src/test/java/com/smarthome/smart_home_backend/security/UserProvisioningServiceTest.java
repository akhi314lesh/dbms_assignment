package com.smarthome.smart_home_backend.security;

import com.google.firebase.auth.FirebaseToken;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.HomeAccessRepository;
import com.smarthome.smart_home_backend.repository.UserRepository;
import com.smarthome.smart_home_backend.service.UserProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import com.smarthome.smart_home_backend.controller.AuthController;
import com.smarthome.smart_home_backend.dto.AuthUserDto;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserProvisioningServiceTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserProvisioningService userProvisioningService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeAccessRepository homeAccessRepository;

    @Test
    @Order(1)
    @DisplayName("Test A: Admin Provisioning matching ADMIN_GOOGLE_EMAIL")
    void testAdminProvisioning() {
        String adminEmail = "akhilesh07vaidya@gmail.com";
        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83";

        FirebaseToken mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(adminUid);
        when(mockToken.getEmail()).thenReturn(adminEmail);
        when(mockToken.getName()).thenReturn("Akhilesh Vaidya");

        User adminUser = userProvisioningService.resolveOrProvisionUser(mockToken);

        assertNotNull(adminUser);
        assertNotNull(adminUser.getUserId());
        assertEquals(adminEmail, adminUser.getEmail());
        assertEquals(adminUid, adminUser.getFirebaseUid());
        assertEquals("ADMIN", adminUser.getRole());
        assertNull(adminUser.getPassword(), "Google-authenticated user password must be NULL");

        // Verify rule 7: No HOMES or HOME_ACCESS records created
        long accessCount = homeAccessRepository.findByUserUserId(adminUser.getUserId()).size();
        assertEquals(0, accessCount, "Newly registered admin must have 0 initial home accesses");
    }

    @Test
    @Order(2)
    @DisplayName("Test B: Standard User Auto-Provisioning (role = USER)")
    void testStandardUserProvisioning() {
        String userEmail = "standard.test.user@example.com";
        String userUid = "firebase-standard-uid-002";

        FirebaseToken mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(userUid);
        when(mockToken.getEmail()).thenReturn(userEmail);
        when(mockToken.getName()).thenReturn("Standard Test User");

        User normalUser = userProvisioningService.resolveOrProvisionUser(mockToken);

        assertNotNull(normalUser);
        assertNotNull(normalUser.getUserId());
        assertEquals(userEmail, normalUser.getEmail());
        assertEquals(userUid, normalUser.getFirebaseUid());
        assertEquals("USER", normalUser.getRole(), "New standard Google user must have role USER");
        assertNull(normalUser.getPassword(), "Google user password must be NULL");

        // Verify rule 7: No HOMES or HOME_ACCESS records created
        long accessCount = homeAccessRepository.findByUserUserId(normalUser.getUserId()).size();
        assertEquals(0, accessCount, "Newly registered user must have 0 initial home accesses");

        // Clean up temporary standard test user so only intentional admin remains
        userRepository.delete(normalUser);
    }

    @Test
    @Order(3)
    @DisplayName("Test C: Idempotent lookup by stable firebase_uid")
    void testIdempotentLookupByUid() {
        String adminEmail = "akhilesh07vaidya@gmail.com";
        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83";

        FirebaseToken mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(adminUid);
        when(mockToken.getEmail()).thenReturn(adminEmail);
        when(mockToken.getName()).thenReturn("Akhilesh Vaidya");

        long countBefore = userRepository.count();
        User resolvedUser = userProvisioningService.resolveOrProvisionUser(mockToken);
        long countAfter = userRepository.count();

        assertEquals(countBefore, countAfter, "Idempotent lookup must not create additional rows");
        assertEquals(adminUid, resolvedUser.getFirebaseUid());
        assertEquals("ADMIN", resolvedUser.getRole());
    }

    @Test
    @Order(4)
    @DisplayName("Test D: Fallback account linking for existing sample user by email")
    void testAccountLinkingForExistingUser() {
        // Use sample user Elena Rostova (user_id = 2, email = elena.rostova@smarthome.io)
        String existingEmail = "elena.rostova@smarthome.io";
        String linkingUid = "firebase-linking-uid-003";

        Optional<User> existingUserOpt = userRepository.findByEmail(existingEmail);
        assertTrue(existingUserOpt.isPresent(), "Sample user must exist");
        User existingUser = existingUserOpt.get();
        assertEquals(2L, existingUser.getUserId());
        assertNull(existingUser.getFirebaseUid(), "Initially sample user firebase_uid is NULL");

        FirebaseToken mockToken = Mockito.mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn(linkingUid);
        when(mockToken.getEmail()).thenReturn(existingEmail);
        when(mockToken.getName()).thenReturn(existingUser.getName());

        long countBefore = userRepository.count();
        User linkedUser = userProvisioningService.resolveOrProvisionUser(mockToken);
        long countAfter = userRepository.count();

        assertEquals(countBefore, countAfter, "Account linking must not create a new row");
        assertEquals(2L, linkedUser.getUserId(), "Must link to the existing user ID 2");
        assertEquals(linkingUid, linkedUser.getFirebaseUid());
        assertEquals("USER", linkedUser.getRole());

        // Restore sample user state so sample data remains completely intact
        linkedUser.setFirebaseUid(null);
        userRepository.save(linkedUser);
    }

    @Test
    @Order(5)
    @DisplayName("Test E: GET /api/auth/me returns safe DTO for authenticated principal")
    void testAuthMeEndpointWithSecurityContext() throws Exception {
        String adminEmail = "akhilesh07vaidya@gmail.com";
        Optional<User> adminOpt = userRepository.findByEmail(adminEmail);
        assertTrue(adminOpt.isPresent(), "Provisioned admin user must be present in database");
        User admin = adminOpt.get();

        FirebaseUserDetails principal = new FirebaseUserDetails(
                admin.getFirebaseUid(),
                admin.getEmail(),
                admin
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            ResponseEntity<AuthUserDto> response = authController.getCurrentUser(authentication);
            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals(admin.getUserId(), response.getBody().getUserId());
            assertEquals(adminEmail, response.getBody().getEmail());
            assertEquals("ADMIN", response.getBody().getRole());
            assertEquals(admin.getFirebaseUid(), response.getBody().getFirebaseUid());
            assertEquals("Akhilesh Vaidya", response.getBody().getName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
