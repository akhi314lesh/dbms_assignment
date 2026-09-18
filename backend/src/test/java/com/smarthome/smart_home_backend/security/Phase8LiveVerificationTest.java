package com.smarthome.smart_home_backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

public class Phase8LiveVerificationTest {

    private static final String FIREBASE_API_KEY = "AIzaSyD3o04kVsWLv6bNgrKMdf-1r878hfbU7WI";
    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void initFirebase() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(isServerListening(), "Backend server on localhost:8080 is not active; skipping live Phase 8 verification");
        if (FirebaseApp.getApps().isEmpty()) {
            String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            File credFile = (credPath != null && !credPath.isBlank()) 
                    ? new File(credPath) 
                    : new File("C:\\Users\\akhil\\OneDrive\\Desktop\\firebase-secrets\\smart-home-service-account.json");
            
            org.junit.jupiter.api.Assumptions.assumeTrue(credFile.exists(), "Firebase service account JSON not present; skipping live Phase 8 verification");
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credFile));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }

    private static boolean isServerListening() {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", 8080), 300);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String getRealFirebaseIdToken(String uid) throws Exception {
        String customToken = FirebaseAuth.getInstance().createCustomToken(uid);
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key=" + FIREBASE_API_KEY;
        String jsonPayload = "{\"token\":\"" + customToken + "\",\"returnSecureToken\":true}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Failed to exchange custom token: " + response.body());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("idToken").getAsString();
    }

    private HttpResponse<String> sendGet(String path, String idToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + idToken)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPut(String path, String jsonBody, String idToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + idToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPost(String path, String jsonBody, String idToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + idToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("Execute Full Phase 8 Live Verification against http://localhost:8080")
    void executeFullLiveVerification() throws Exception {
        System.out.println("===============================================================");
        System.out.println("   PHASE 8 FULL LIVE VERIFICATION WITH REAL FIREBASE ID TOKENS  ");
        System.out.println("===============================================================");

        // -------------------------------------------------------------
        // 1. LIVE ADMIN TEST (akhilesh07vaidya@gmail.com, UID: 6bITkR1RdRgzCG1lw0C6nIQAan83)
        // -------------------------------------------------------------
        System.out.println("\n--- 1. LIVE ADMIN VERIFICATION ---");
        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83";
        String adminToken = getRealFirebaseIdToken(adminUid);
        assertNotNull(adminToken, "Admin real Firebase ID token must be non-null");
        System.out.println("[TOKEN] Successfully obtained real Google Firebase ID Token for Admin (akhilesh07vaidya@gmail.com)");

        // GET /api/homes -> 200 and all 5 homes
        HttpResponse<String> homesRes = sendGet("/api/homes", adminToken);
        System.out.println("ADMIN GET /api/homes -> Status: " + homesRes.statusCode() + " | Body length: " + homesRes.body().length());
        assertEquals(200, homesRes.statusCode());
        assertTrue(homesRes.body().contains("\"homeId\":1"));
        assertTrue(homesRes.body().contains("\"homeId\":5"));

        // GET /api/rooms -> 200 and all 22 rooms
        HttpResponse<String> roomsRes = sendGet("/api/rooms", adminToken);
        System.out.println("ADMIN GET /api/rooms -> Status: " + roomsRes.statusCode() + " | Body length: " + roomsRes.body().length());
        assertEquals(200, roomsRes.statusCode());

        // GET /api/devices -> 200 and all 45 devices
        HttpResponse<String> devicesRes = sendGet("/api/devices", adminToken);
        System.out.println("ADMIN GET /api/devices -> Status: " + devicesRes.statusCode() + " | Body length: " + devicesRes.body().length());
        assertEquals(200, devicesRes.statusCode());

        // GET /api/readings -> 200 and all 120 readings
        HttpResponse<String> readingsRes = sendGet("/api/readings", adminToken);
        System.out.println("ADMIN GET /api/readings -> Status: " + readingsRes.statusCode() + " | Body length: " + readingsRes.body().length());
        assertEquals(200, readingsRes.statusCode());

        // GET /api/alerts -> 200 and all 35 alerts
        HttpResponse<String> alertsRes = sendGet("/api/alerts", adminToken);
        System.out.println("ADMIN GET /api/alerts -> Status: " + alertsRes.statusCode() + " | Body length: " + alertsRes.body().length());
        assertEquals(200, alertsRes.statusCode());

        // GET /api/dashboard/summary -> 200 with global totals
        HttpResponse<String> dashRes = sendGet("/api/dashboard/summary", adminToken);
        System.out.println("ADMIN GET /api/dashboard/summary -> Status: " + dashRes.statusCode() + " | Body: " + dashRes.body());
        assertEquals(200, dashRes.statusCode());
        assertTrue(dashRes.body().contains("\"totalHomes\":5"));
        assertTrue(dashRes.body().contains("\"totalDevices\":45"));
        assertTrue(dashRes.body().contains("\"totalAlerts\":35"));

        // GET /api/users -> 200
        HttpResponse<String> usersRes = sendGet("/api/users", adminToken);
        System.out.println("ADMIN GET /api/users -> Status: " + usersRes.statusCode() + " | Body length: " + usersRes.body().length());
        assertEquals(200, usersRes.statusCode());

        // -------------------------------------------------------------
        // 2. LIVE NORMAL USER TEST (Sophia Chen: sophia.chen@smarthome.io, user_id = 4)
        // -------------------------------------------------------------
        System.out.println("\n--- 2. LIVE NORMAL USER VERIFICATION (Home 1 Access Only) ---");
        String userEmail = "sophia.chen@smarthome.io";
        UserRecord userRecord;
        try {
            userRecord = FirebaseAuth.getInstance().getUserByEmail(userEmail);
        } catch (FirebaseAuthException e) {
            UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                    .setEmail(userEmail)
                    .setEmailVerified(true)
                    .setDisplayName("Sophia Chen");
            userRecord = FirebaseAuth.getInstance().createUser(req);
        }

        String userToken = getRealFirebaseIdToken(userRecord.getUid());
        assertNotNull(userToken, "User real Firebase ID token must be non-null");
        System.out.println("[TOKEN] Successfully obtained real Google Firebase ID Token for Normal User (" + userEmail + ", UID: " + userRecord.getUid() + ")");

        // Home 1 -> 200
        HttpResponse<String> userHome1Res = sendGet("/api/homes/1", userToken);
        System.out.println("USER GET /api/homes/1 -> Status: " + userHome1Res.statusCode() + " | Body: " + userHome1Res.body());
        assertEquals(200, userHome1Res.statusCode());

        // Home 2 -> 403
        HttpResponse<String> userHome2Res = sendGet("/api/homes/2", userToken);
        System.out.println("USER GET /api/homes/2 -> Status: " + userHome2Res.statusCode() + " | Body: " + userHome2Res.body());
        assertEquals(403, userHome2Res.statusCode());

        // Room in Home 1 -> 200 (Room 1)
        HttpResponse<String> userRoom1Res = sendGet("/api/rooms/1", userToken);
        System.out.println("USER GET /api/rooms/1 -> Status: " + userRoom1Res.statusCode() + " | Body: " + userRoom1Res.body());
        assertEquals(200, userRoom1Res.statusCode());

        // Room in Home 2 -> 403 (Room 4)
        HttpResponse<String> userRoom4Res = sendGet("/api/rooms/4", userToken);
        System.out.println("USER GET /api/rooms/4 -> Status: " + userRoom4Res.statusCode() + " | Body: " + userRoom4Res.body());
        assertEquals(403, userRoom4Res.statusCode());

        // Device in Home 1 -> 200 (Device 1)
        HttpResponse<String> userDevice1Res = sendGet("/api/devices/1", userToken);
        System.out.println("USER GET /api/devices/1 -> Status: " + userDevice1Res.statusCode() + " | Body: " + userDevice1Res.body());
        assertEquals(200, userDevice1Res.statusCode());

        // Device in Home 2 -> 403 (Device 10)
        HttpResponse<String> userDevice10Res = sendGet("/api/devices/10", userToken);
        System.out.println("USER GET /api/devices/10 -> Status: " + userDevice10Res.statusCode() + " | Body: " + userDevice10Res.body());
        assertEquals(403, userDevice10Res.statusCode());

        // Alert in Home 1 -> 200 (Alert 1)
        HttpResponse<String> userAlert1Res = sendGet("/api/alerts/1", userToken);
        System.out.println("USER GET /api/alerts/1 -> Status: " + userAlert1Res.statusCode() + " | Body: " + userAlert1Res.body());
        assertEquals(200, userAlert1Res.statusCode());

        // Alert in Home 2 -> 403 (Alert 3)
        HttpResponse<String> userAlert3Res = sendGet("/api/alerts/3", userToken);
        System.out.println("USER GET /api/alerts/3 -> Status: " + userAlert3Res.statusCode() + " | Body: " + userAlert3Res.body());
        assertEquals(403, userAlert3Res.statusCode());

        // Dashboard summary contains only Home 1 data
        HttpResponse<String> userDashRes = sendGet("/api/dashboard/summary", userToken);
        System.out.println("USER GET /api/dashboard/summary -> Status: " + userDashRes.statusCode() + " | Body: " + userDashRes.body());
        assertEquals(200, userDashRes.statusCode());
        assertTrue(userDashRes.body().contains("\"totalHomes\":1"));
        assertFalse(userDashRes.body().contains("\"totalHomes\":5"));

        // -------------------------------------------------------------
        // 3. LIVE MUTATION AUTHORIZATION (Real User Token)
        // -------------------------------------------------------------
        System.out.println("\n--- 3. LIVE MUTATION AUTHORIZATION ATTEMPTS (Expected 403 Forbidden) ---");

        // Mutate a device in unauthorized Home 2 (Device 10) -> 403
        String updateDeviceJson = "{\"deviceName\":\"Compromised Device\",\"status\":\"OFFLINE\"}";
        HttpResponse<String> mutateDevRes = sendPut("/api/devices/10", updateDeviceJson, userToken);
        System.out.println("USER PUT /api/devices/10 (Home 2) -> Status: " + mutateDevRes.statusCode() + " | Body: " + mutateDevRes.body());
        assertEquals(403, mutateDevRes.statusCode());

        // Access another user's profile (/api/users/1) -> 403
        HttpResponse<String> otherUserRes = sendGet("/api/users/1", userToken);
        System.out.println("USER GET /api/users/1 (User 1 profile) -> Status: " + otherUserRes.statusCode() + " | Body: " + otherUserRes.body());
        assertEquals(403, otherUserRes.statusCode());

        // Modify another user's notification preference (/api/notification-preferences/1/1/1) -> 403
        String updatePrefJson = "{\"smsEnabled\":1,\"emailEnabled\":1,\"pushEnabled\":1}";
        HttpResponse<String> mutatePrefRes = sendPut("/api/notification-preferences/1/1/1", updatePrefJson, userToken);
        System.out.println("USER PUT /api/notification-preferences/1/1/1 -> Status: " + mutatePrefRes.statusCode() + " | Body: " + mutatePrefRes.body());
        assertEquals(403, mutatePrefRes.statusCode());

        // Create/modify automation targeting an inaccessible device (Device 10 in Home 2) -> 403
        String actionJson = "{\"actionType\":\"TURN_OFF\",\"actionValue\":\"NOW\"}";
        HttpResponse<String> mutateActionRes = sendPost("/api/rules/1/actions/device/10", actionJson, userToken);
        System.out.println("USER POST /api/rules/1/actions/device/10 -> Status: " + mutateActionRes.statusCode() + " | Body: " + mutateActionRes.body());
        assertEquals(403, mutateActionRes.statusCode());

        System.out.println("\n===============================================================");
        System.out.println("   ALL LIVE TESTS COMPLETED AND PASSED WITH REAL FIREBASE TOKENS! ");
        System.out.println("===============================================================");
    }
}
