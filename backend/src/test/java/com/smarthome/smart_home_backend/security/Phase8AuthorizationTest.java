package com.smarthome.smart_home_backend.security;

import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class Phase8AuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken user4Auth; // Sophia Chen (Home 1 member only)

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Admin: user_id = 11, role = ADMIN, 0 HOME_ACCESS rows
        User adminUser = userRepository.findByEmail("akhilesh07vaidya@gmail.com")
                .orElseThrow(() -> new IllegalStateException("Admin user akhilesh07vaidya@gmail.com not found"));
        FirebaseUserDetails adminPrincipal = new FirebaseUserDetails(
                adminUser.getFirebaseUid(),
                adminUser.getEmail(),
                adminUser
        );
        adminAuth = new UsernamePasswordAuthenticationToken(
                adminPrincipal,
                null,
                adminPrincipal.getAuthorities()
        );

        // Standard User: user_id = 4, role = USER, member of Home 1 ONLY (no access to Home 2, 3, 4, 5)
        User user4 = userRepository.findById(4L)
                .orElseThrow(() -> new IllegalStateException("Sample user 4 (sophia.chen@smarthome.io) not found"));
        FirebaseUserDetails user4Principal = new FirebaseUserDetails(
                "mock-firebase-uid-user-4",
                user4.getEmail(),
                user4
        );
        user4Auth = new UsernamePasswordAuthenticationToken(
                user4Principal,
                null,
                user4Principal.getAuthorities()
        );
    }

    @Nested
    @DisplayName("1. Unauthenticated Access Protection (401 Unauthorized)")
    class UnauthenticatedAccessTests {

        @Test
        @DisplayName("Unauthenticated request to /api/homes returns 401")
        void testUnauthenticatedHomesFails() throws Exception {
            mockMvc.perform(get("/api/homes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to /api/devices returns 401")
        void testUnauthenticatedDevicesFails() throws Exception {
            mockMvc.perform(get("/api/devices"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated request to /api/dashboard/summary returns 401")
        void testUnauthenticatedDashboardFails() throws Exception {
            mockMvc.perform(get("/api/dashboard/summary"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("2. Admin Privilege & Universal Access (Role = ADMIN, 0 HOME_ACCESS)")
    class AdminUniversalAccessTests {

        @Test
        @DisplayName("Admin sees all 5 homes in GET /api/homes")
        void testAdminListsAllHomes() throws Exception {
            mockMvc.perform(get("/api/homes").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(5)));
        }

        @Test
        @DisplayName("Admin can directly access Home 1 and Home 2")
        void testAdminAccessesAnyHome() throws Exception {
            mockMvc.perform(get("/api/homes/1").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.homeId").value(1));

            mockMvc.perform(get("/api/homes/2").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.homeId").value(2));
        }

        @Test
        @DisplayName("Admin sees all 22 rooms in GET /api/rooms")
        void testAdminListsAllRooms() throws Exception {
            mockMvc.perform(get("/api/rooms").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(22)));
        }

        @Test
        @DisplayName("Admin can access Room 4 in Home 2")
        void testAdminAccessesRoomInHome2() throws Exception {
            mockMvc.perform(get("/api/rooms/4").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value(4));
        }

        @Test
        @DisplayName("Admin sees all 45 devices in GET /api/devices")
        void testAdminListsAllDevices() throws Exception {
            mockMvc.perform(get("/api/devices").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(45)));
        }

        @Test
        @DisplayName("Admin can access Device 10 in Home 2")
        void testAdminAccessesDeviceInHome2() throws Exception {
            mockMvc.perform(get("/api/devices/10").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value(10));
        }

        @Test
        @DisplayName("Admin sees all 35 alerts in GET /api/alerts")
        void testAdminListsAllAlerts() throws Exception {
            mockMvc.perform(get("/api/alerts").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(35)));
        }

        @Test
        @DisplayName("Admin can access Alert 3 in Home 2")
        void testAdminAccessesAlertInHome2() throws Exception {
            mockMvc.perform(get("/api/alerts/3").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.alertId").value(3));
        }

        @Test
        @DisplayName("Admin dashboard summary reflects global counts (totalHomes = 5)")
        void testAdminDashboardSummary() throws Exception {
            mockMvc.perform(get("/api/dashboard/summary").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHomes").value(5))
                    .andExpect(jsonPath("$.totalDevices").value(45))
                    .andExpect(jsonPath("$.totalAlerts").value(35));
        }

        @Test
        @DisplayName("Admin can list all users in GET /api/users")
        void testAdminListsAllUsers() throws Exception {
            mockMvc.perform(get("/api/users").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(8))));
        }

        @Test
        @DisplayName("Admin can view any user profile GET /api/users/1")
        void testAdminViewsOtherUserProfile() throws Exception {
            mockMvc.perform(get("/api/users/1").with(authentication(adminAuth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(1));
        }
    }

    @Nested
    @DisplayName("3. Standard User Accessible Resources (User 4 -> Home 1 Only)")
    class UserAuthorizedAccessTests {

        @Test
        @DisplayName("User 4 lists ONLY Home 1 in GET /api/homes")
        void testUserListsOnlyAccessibleHome() throws Exception {
            mockMvc.perform(get("/api/homes").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].homeId").value(1));
        }

        @Test
        @DisplayName("User 4 can access Home 1 directly in GET /api/homes/1")
        void testUserAccessesHome1() throws Exception {
            mockMvc.perform(get("/api/homes/1").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.homeId").value(1));
        }

        @Test
        @DisplayName("User 4 lists only rooms in Home 1")
        void testUserListsRoomsInHome1() throws Exception {
            mockMvc.perform(get("/api/rooms").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$[*].home.homeId", everyItem(equalTo(1))));
        }

        @Test
        @DisplayName("User 4 can access Room 1 (in Home 1)")
        void testUserAccessesRoom1() throws Exception {
            mockMvc.perform(get("/api/rooms/1").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roomId").value(1));
        }

        @Test
        @DisplayName("User 4 lists only devices in Home 1")
        void testUserListsDevicesInHome1() throws Exception {
            mockMvc.perform(get("/api/devices").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$[*].room.home.homeId", everyItem(equalTo(1))));
        }

        @Test
        @DisplayName("User 4 can access Device 1 (in Home 1)")
        void testUserAccessesDevice1() throws Exception {
            mockMvc.perform(get("/api/devices/1").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceId").value(1));
        }

        @Test
        @DisplayName("User 4 dashboard summary reflects only Home 1 (totalHomes = 1)")
        void testUserDashboardSummary() throws Exception {
            mockMvc.perform(get("/api/dashboard/summary").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHomes").value(1))
                    .andExpect(jsonPath("$.totalDevices").value(lessThan(45)));
        }

        @Test
        @DisplayName("User 4 can view own profile GET /api/users/4")
        void testUserViewsOwnProfile() throws Exception {
            mockMvc.perform(get("/api/users/4").with(authentication(user4Auth)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(4));
        }
    }

    @Nested
    @DisplayName("4. IDOR Prevention & Isolation (User 4 -> Home 2 Returns 403 Forbidden)")
    class IdorPreventionTests {

        @Test
        @DisplayName("User 4 cannot access Home 2 (403 Forbidden)")
        void testUserCannotAccessHome2() throws Exception {
            mockMvc.perform(get("/api/homes/2").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"))
                    .andExpect(jsonPath("$.message").value(containsString("Access is denied")));
        }

        @Test
        @DisplayName("User 4 cannot access Room 4 in Home 2 (403 Forbidden)")
        void testUserCannotAccessRoom4InHome2() throws Exception {
            mockMvc.perform(get("/api/rooms/4").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot access Device 10 in Home 2 (403 Forbidden)")
        void testUserCannotAccessDevice10InHome2() throws Exception {
            mockMvc.perform(get("/api/devices/10").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot access Alert 3 in Home 2 (403 Forbidden)")
        void testUserCannotAccessAlert3InHome2() throws Exception {
            mockMvc.perform(get("/api/alerts/3").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot access readings of Device 8 in Home 2 (403 Forbidden)")
        void testUserCannotAccessReadingDevice8InHome2() throws Exception {
            mockMvc.perform(get("/api/readings/device/8").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot access rule actions of Device 10 in Home 2 (403 Forbidden)")
        void testUserCannotAccessActionsDevice10InHome2() throws Exception {
            mockMvc.perform(get("/api/rules/actions/device/10").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot view User 1 profile GET /api/users/1 (403 Forbidden)")
        void testUserCannotViewOtherUserProfile() throws Exception {
            mockMvc.perform(get("/api/users/1").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot list all users GET /api/users (403 Forbidden)")
        void testUserCannotListAllUsers() throws Exception {
            mockMvc.perform(get("/api/users").with(authentication(user4Auth)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }

        @Test
        @DisplayName("User 4 cannot create alert category (403 Forbidden)")
        void testUserCannotCreateAlertCategory() throws Exception {
            String newCategoryJson = "{\"categoryName\":\"Unauthorized Category\",\"severity\":\"LOW\"}";
            mockMvc.perform(post("/api/alert-categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(newCategoryJson)
                            .with(authentication(user4Auth)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("User 4 cannot grant access to Home 2 (403 Forbidden)")
        void testUserCannotGrantAccessToHome2() throws Exception {
            String grantJson = "{\"role\":\"MEMBER\"}";
            mockMvc.perform(post("/api/home-access/2/2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(grantJson)
                            .with(authentication(user4Auth)))
                    .andExpect(status().isForbidden());
        }
    }
}
