package com.smarthome.smart_home_backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.gson.JsonArray;
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

public class Phase9LiveVerificationTest {

    private static final String FIREBASE_API_KEY = "AIzaSyD3o04kVsWLv6bNgrKMdf-1r878hfbU7WI";
    private static final String BASE_URL = "http://localhost:8080";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void initFirebase() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(isServerListening(), "Backend server on localhost:8080 is not active; skipping live Phase 9 verification");
        if (FirebaseApp.getApps().isEmpty()) {
            String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            File credFile = (credPath != null && !credPath.isBlank()) 
                    ? new File(credPath) 
                    : new File("C:\\Users\\akhil\\OneDrive\\Desktop\\firebase-secrets\\smart-home-service-account.json");
            
            org.junit.jupiter.api.Assumptions.assumeTrue(credFile.exists(), "Firebase service account JSON not present; skipping live Phase 9 verification");
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
    @DisplayName("Execute Comprehensive Phase 9 Live Verification with Real Firebase ID Tokens")
    void executePhase9Verification() throws Exception {
        System.out.println("===============================================================");
        System.out.println("   PHASE 9 FULL LIVE END-TO-END VERIFICATION                  ");
        System.out.println("===============================================================");

        // -------------------------------------------------------------
        // A. REAL ADMIN TOKENS & LIVE ENDPOINTS
        // -------------------------------------------------------------
        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83";
        String adminToken = getRealFirebaseIdToken(adminUid);
        assertNotNull(adminToken);
        System.out.println("[AUTH] Successfully acquired real Firebase ID token for ADMIN (akhilesh07vaidya@gmail.com)");

        // 1. Dashboard summary with deviceBreakdown
        HttpResponse<String> summaryRes = sendGet("/api/dashboard/summary", adminToken);
        assertEquals(200, summaryRes.statusCode());
        JsonObject summaryJson = JsonParser.parseString(summaryRes.body()).getAsJsonObject();
        assertEquals(5, summaryJson.get("totalHomes").getAsInt());
        assertEquals(22, summaryJson.get("totalRooms").getAsInt());
        assertEquals(45, summaryJson.get("totalDevices").getAsInt());
        assertEquals(35, summaryJson.get("totalAlerts").getAsInt());
        assertTrue(summaryJson.has("deviceBreakdown"));
        JsonObject breakdown = summaryJson.getAsJsonObject("deviceBreakdown");
        assertEquals(10, breakdown.get("smartLights").getAsInt());
        assertEquals(5, breakdown.get("thermostats").getAsInt());
        assertEquals(10, breakdown.get("temperatureSensors").getAsInt());
        assertEquals(8, breakdown.get("motionSensors").getAsInt());
        assertEquals(7, breakdown.get("cameras").getAsInt());
        assertEquals(5, breakdown.get("hubs").getAsInt());
        System.out.println("[DASHBOARD] Live summary verified with device breakdown: " + breakdown);

        // 2. Detailed devices for Home 1
        HttpResponse<String> detailedRes = sendGet("/api/devices/home/1/detailed", adminToken);
        assertEquals(200, detailedRes.statusCode());
        JsonArray detailedDevices = JsonParser.parseString(detailedRes.body()).getAsJsonArray();
        assertTrue(detailedDevices.size() > 0, "Home 1 must have detailed devices");
        System.out.println("[DEVICES] Found " + detailedDevices.size() + " detailed devices in Home 1");

        boolean foundSmartLight = false, foundThermostat = false, foundTempSensor = false, foundMotion = false, foundCamera = false;
        for (int i = 0; i < detailedDevices.size(); i++) {
            JsonObject d = detailedDevices.get(i).getAsJsonObject();
            String subtype = d.get("deviceSubtype").getAsString();
            assertTrue(d.has("uptime") && !d.get("uptime").isJsonNull(), "Uptime must be computed via PL/SQL function");
            switch (subtype.toUpperCase().replace("_", "")) {
                case "SMARTLIGHT" -> {
                    foundSmartLight = true;
                    assertTrue(d.has("brightness") || d.has("colorSupport"));
                }
                case "THERMOSTAT" -> {
                    foundThermostat = true;
                    assertTrue(d.has("targetTemperature") || d.has("thermostatMode"));
                }
                case "TEMPERATURESENSOR" -> {
                    foundTempSensor = true;
                    assertTrue(d.has("unit") || d.has("latestReadingValue") || d.has("minRange"));
                }
                case "MOTIONSENSOR" -> {
                    foundMotion = true;
                    assertTrue(d.has("sensitivityLevel") || d.has("detectionRange"));
                }
                case "CAMERA" -> {
                    foundCamera = true;
                    assertTrue(d.has("resolution") || d.has("storageType"));
                }
            }
        }
        System.out.println("[DEVICES] Subtypes verified (SmartLight=" + foundSmartLight + ", Thermostat=" + foundThermostat +
                ", TempSensor=" + foundTempSensor + ", Motion=" + foundMotion + ", Camera=" + foundCamera + ")");

        // 3. Oracle Stored Procedure: sp_home_device_health_report
        HttpResponse<String> healthRes = sendGet("/api/homes/1/health-report", adminToken);
        assertEquals(200, healthRes.statusCode());
        JsonObject healthJson = JsonParser.parseString(healthRes.body()).getAsJsonObject();
        assertTrue(healthJson.has("reportText"));
        String reportText = healthJson.get("reportText").getAsString();
        assertTrue(reportText.contains("DEVICE HEALTH AND STATUS REPORT") || reportText.contains("Summary:"), "Health report text must be returned");
        System.out.println("[STORED PROC] sp_home_device_health_report verified:\n" + reportText);

        // 4. Oracle Stored Procedure: sp_acknowledge_alert
        HttpResponse<String> ackRes = sendPost("/api/alerts/1/acknowledge", "{}", adminToken);
        assertEquals(200, ackRes.statusCode());
        JsonObject ackJson = JsonParser.parseString(ackRes.body()).getAsJsonObject();
        assertEquals(1, ackJson.get("alertId").getAsLong());
        System.out.println("[STORED PROC] sp_acknowledge_alert(1, userId) verified successfully.");

        // -------------------------------------------------------------
        // B. ADMIN SQL CONSOLE & REVERSIBLE DML TEST
        // -------------------------------------------------------------
        System.out.println("\n--- B. ADMIN SQL CONSOLE ---");
        // ADMIN SELECT
        String adminSelectBody = "{\"sql\":\"SELECT COUNT(*) as total_homes FROM HOMES\"}";
        HttpResponse<String> sqlSelectRes = sendPost("/api/sql-console/execute", adminSelectBody, adminToken);
        assertEquals(200, sqlSelectRes.statusCode());
        JsonObject sqlSelectJson = JsonParser.parseString(sqlSelectRes.body()).getAsJsonObject();
        assertTrue(sqlSelectJson.get("success").getAsBoolean());
        System.out.println("[SQL CONSOLE] ADMIN SELECT verified: " + sqlSelectJson.get("rows"));

        // ADMIN REVERSIBLE DML TEST
        String verifyInsertSql = "{\"sql\":\"SELECT home_id, home_name FROM HOMES WHERE home_id = 999\"}";
        try {
            // Step 1: INSERT temporary home
            String insertSql = "{\"sql\":\"INSERT INTO HOMES (home_id, home_name, street, city, pincode) VALUES (999, 'Temporary Admin Test Home', '123 Temp Road', 'Test City', '12345')\"}";
            HttpResponse<String> insertRes = sendPost("/api/sql-console/execute", insertSql, adminToken);
            assertEquals(200, insertRes.statusCode());
            JsonObject insertJson = JsonParser.parseString(insertRes.body()).getAsJsonObject();
            assertTrue(insertJson.get("success").getAsBoolean());
            assertEquals(1, insertJson.get("affectedRows").getAsInt());
            System.out.println("[SQL CONSOLE] ADMIN INSERT temporary record verified: affectedRows = 1");

            // Step 2: VERIFY record exists
            HttpResponse<String> verifyInsertRes = sendPost("/api/sql-console/execute", verifyInsertSql, adminToken);
            assertEquals(200, verifyInsertRes.statusCode());
            JsonObject verifyInsertJson = JsonParser.parseString(verifyInsertRes.body()).getAsJsonObject();
            assertEquals(1, verifyInsertJson.getAsJsonArray("rows").size());
            System.out.println("[SQL CONSOLE] Temporary record verified present in Oracle");

            // Step 3: UPDATE temporary record
            String updateSql = "{\"sql\":\"UPDATE HOMES SET home_name = 'Temporary Admin Test Home Updated' WHERE home_id = 999\"}";
            HttpResponse<String> updateRes = sendPost("/api/sql-console/execute", updateSql, adminToken);
            assertEquals(200, updateRes.statusCode());
            JsonObject updateJson = JsonParser.parseString(updateRes.body()).getAsJsonObject();
            assertTrue(updateJson.get("success").getAsBoolean());
            assertEquals(1, updateJson.get("affectedRows").getAsInt());
            System.out.println("[SQL CONSOLE] ADMIN UPDATE temporary record verified: affectedRows = 1");
        } finally {
            // Step 4: DELETE temporary record to revert back to original state
            String deleteSql = "{\"sql\":\"DELETE FROM HOMES WHERE home_id = 999\"}";
            HttpResponse<String> deleteRes = sendPost("/api/sql-console/execute", deleteSql, adminToken);
            assertEquals(200, deleteRes.statusCode());
            JsonObject deleteJson = JsonParser.parseString(deleteRes.body()).getAsJsonObject();
            assertTrue(deleteJson.get("success").getAsBoolean());
            System.out.println("[SQL CONSOLE] ADMIN DELETE temporary record verified");

            // Step 5: VERIFY record is gone
            HttpResponse<String> verifyGoneRes = sendPost("/api/sql-console/execute", verifyInsertSql, adminToken);
            assertEquals(200, verifyGoneRes.statusCode());
            JsonObject verifyGoneJson = JsonParser.parseString(verifyGoneRes.body()).getAsJsonObject();
            assertEquals(0, verifyGoneJson.getAsJsonArray("rows").size());
            System.out.println("[SQL CONSOLE] Temporary record verified completely removed. Baseline restored.");
        }

        // ADMIN PROHIBITED DDL
        String ddlSql = "{\"sql\":\"DROP TABLE ROOMS\"}";
        HttpResponse<String> ddlRes = sendPost("/api/sql-console/execute", ddlSql, adminToken);
        assertEquals(200, ddlRes.statusCode());
        JsonObject ddlJson = JsonParser.parseString(ddlRes.body()).getAsJsonObject();
        assertFalse(ddlJson.get("success").getAsBoolean());
        assertTrue(ddlJson.get("message").getAsString().contains("DROP") || ddlJson.get("message").getAsString().contains("prohibited"));
        System.out.println("[SQL CONSOLE] ADMIN DDL correctly blocked (success=false, message=" + ddlJson.get("message").getAsString() + ")");

        // -------------------------------------------------------------
        // C. USER SQL CONSOLE & SECURITY RESTRICTIONS
        // -------------------------------------------------------------
        System.out.println("\n--- C. USER SQL CONSOLE & DATA ISOLATION ---");
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
        assertNotNull(userToken);
        System.out.println("[AUTH] Successfully acquired real Firebase ID token for USER (sophia.chen@smarthome.io)");

        // 1. Scoped SELECT on HOMES
        String userHomesSql = "{\"sql\":\"SELECT * FROM HOMES\"}";
        HttpResponse<String> userHomesRes = sendPost("/api/sql-console/execute", userHomesSql, userToken);
        assertEquals(200, userHomesRes.statusCode());
        JsonObject userHomesJson = JsonParser.parseString(userHomesRes.body()).getAsJsonObject();
        JsonArray userHomeRows = userHomesJson.getAsJsonArray("rows");
        assertEquals(1, userHomeRows.size(), "USER Sophia Chen must only see Home 1");
        System.out.println("[SQL CONSOLE] USER SELECT * FROM HOMES returns exactly 1 row (Home 1)");

        // 2. Cross-home attempt on HOMES
        String userCrossHomeSql = "{\"sql\":\"SELECT * FROM HOMES WHERE home_id = 2\"}";
        HttpResponse<String> userCrossHomeRes = sendPost("/api/sql-console/execute", userCrossHomeSql, userToken);
        assertEquals(200, userCrossHomeRes.statusCode());
        JsonObject userCrossHomeJson = JsonParser.parseString(userCrossHomeRes.body()).getAsJsonObject();
        assertEquals(0, userCrossHomeJson.getAsJsonArray("rows").size(), "Foreign home must yield 0 rows");
        System.out.println("[SQL CONSOLE] USER query for foreign home_id = 2 returns 0 rows");

        // 3. User SELECT on Prohibited table USERS
        String userProhibitedSql = "{\"sql\":\"SELECT * FROM USERS\"}";
        HttpResponse<String> userProhibitedRes = sendPost("/api/sql-console/execute", userProhibitedSql, userToken);
        assertEquals(200, userProhibitedRes.statusCode());
        JsonObject userProhibitedJson = JsonParser.parseString(userProhibitedRes.body()).getAsJsonObject();
        assertFalse(userProhibitedJson.get("success").getAsBoolean());
        assertTrue(userProhibitedJson.get("message").getAsString().toLowerCase().contains("restricted") || userProhibitedJson.get("message").getAsString().toLowerCase().contains("prohibited"));
        System.out.println("[SQL CONSOLE] USER query on USERS table rejected (success=false): " + userProhibitedJson.get("message").getAsString());

        // 4. User SELECT on Prohibited table HOME_ACCESS
        String userProhibitedHaSql = "{\"sql\":\"SELECT * FROM HOME_ACCESS\"}";
        HttpResponse<String> userProhibitedHaRes = sendPost("/api/sql-console/execute", userProhibitedHaSql, userToken);
        assertEquals(200, userProhibitedHaRes.statusCode());
        JsonObject userProhibitedHaJson = JsonParser.parseString(userProhibitedHaRes.body()).getAsJsonObject();
        assertFalse(userProhibitedHaJson.get("success").getAsBoolean());
        assertTrue(userProhibitedHaJson.get("message").getAsString().toLowerCase().contains("restricted") || userProhibitedHaJson.get("message").getAsString().toLowerCase().contains("prohibited"));
        System.out.println("[SQL CONSOLE] USER query on HOME_ACCESS table rejected (success=false): " + userProhibitedHaJson.get("message").getAsString());

        // 5. User DML Attempt (UPDATE)
        String userUpdateSql = "{\"sql\":\"UPDATE HOMES SET home_name = 'Hacked' WHERE home_id = 1\"}";
        HttpResponse<String> userUpdateRes = sendPost("/api/sql-console/execute", userUpdateSql, userToken);
        assertEquals(200, userUpdateRes.statusCode());
        JsonObject userUpdateJson = JsonParser.parseString(userUpdateRes.body()).getAsJsonObject();
        assertFalse(userUpdateJson.get("success").getAsBoolean());
        assertTrue(userUpdateJson.get("message").getAsString().contains("SELECT"));
        System.out.println("[SQL CONSOLE] USER UPDATE query rejected (success=false): " + userUpdateJson.get("message").getAsString());

        // 6. User DML Attempt (INSERT)
        String userInsertSql = "{\"sql\":\"INSERT INTO ROOMS (room_id, room_name, home_id) VALUES (999, 'Bad', 1)\"}";
        HttpResponse<String> userInsertRes = sendPost("/api/sql-console/execute", userInsertSql, userToken);
        assertEquals(200, userInsertRes.statusCode());
        JsonObject userInsertJson = JsonParser.parseString(userInsertRes.body()).getAsJsonObject();
        assertFalse(userInsertJson.get("success").getAsBoolean());
        assertTrue(userInsertJson.get("message").getAsString().contains("SELECT"));
        System.out.println("[SQL CONSOLE] USER INSERT query rejected (success=false): " + userInsertJson.get("message").getAsString());

        // 7. User API Foreign Access -> 403
        HttpResponse<String> userForeignHomeRes = sendGet("/api/homes/2", userToken);
        assertEquals(403, userForeignHomeRes.statusCode());
        HttpResponse<String> userForeignRoomsRes = sendGet("/api/rooms/home/2", userToken);
        assertEquals(403, userForeignRoomsRes.statusCode());
        HttpResponse<String> userForeignDevicesRes = sendGet("/api/devices/home/2/detailed", userToken);
        assertEquals(403, userForeignDevicesRes.statusCode());
        HttpResponse<String> userForeignHealthRes = sendGet("/api/homes/2/health-report", userToken);
        assertEquals(403, userForeignHealthRes.statusCode());
        System.out.println("[AUTHORIZATION] Foreign home endpoints verified: All return 403 Forbidden for regular user.");

        System.out.println("\n===============================================================");
        System.out.println("   PHASE 9 LIVE VERIFICATION COMPLETED WITH 100% PASS RATE     ");
        System.out.println("===============================================================");
    }
}
