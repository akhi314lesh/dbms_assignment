package com.smarthome.smart_home_backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NewmanRunnerTest {

    private static final String FIREBASE_API_KEY = "AIzaSyD3o04kVsWLv6bNgrKMdf-1r878hfbU7WI";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @BeforeAll
    static void initFirebase() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(isServerListening(), "Backend server on localhost:8080 is not active; skipping live Newman test");
        if (FirebaseApp.getApps().isEmpty()) {
            String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            File credFile = (credPath != null && !credPath.isBlank()) 
                    ? new File(credPath) 
                    : new File("C:\\Users\\akhil\\OneDrive\\Desktop\\firebase-secrets\\smart-home-service-account.json");
            
            org.junit.jupiter.api.Assumptions.assumeTrue(credFile.exists(), "Firebase service account JSON not present; skipping live Postman test");
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

    private static String getRealFirebaseIdToken(String uid) throws Exception {
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

    @Test
    @DisplayName("Execute Phase 9.3 Postman Collection via Newman CLI with Real Firebase ID Tokens")
    void executeNewmanPostmanCollection() throws Exception {
        System.out.println("===============================================================");
        System.out.println("   PHASE 9.3 LIVE POSTMAN / NEWMAN VERIFICATION RUNNER        ");
        System.out.println("===============================================================");

        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83"; // akhilesh07vaidya@gmail.com
        String userUid = "n1PL4z3BhAdWp1PT5XxJKuIuvUm1";  // sophia.chen@smarthome.io

        String adminToken = getRealFirebaseIdToken(adminUid);
        assertNotNull(adminToken, "Admin ID token must not be null");
        System.out.println("[AUTH] Successfully acquired real Firebase ID token for ADMIN");

        String userToken = getRealFirebaseIdToken(userUid);
        assertNotNull(userToken, "User ID token must not be null");
        System.out.println("[AUTH] Successfully acquired real Firebase ID token for USER (Sophia Chen)");

        File workspaceRoot = new File("c:\\Users\\akhil\\OneDrive\\Desktop\\Projects\\dbms");
        File collectionFile = new File(workspaceRoot, "docs\\postman\\SmartHome-API.postman_collection.json");
        File environmentTemplate = new File(workspaceRoot, "docs\\postman\\SmartHome-Local.postman_environment.json");

        // Prepare ephemeral environment file inside target/ (gitignored)
        File targetDir = new File(workspaceRoot, "backend\\target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File ephemeralEnvFile = new File(targetDir, "ephemeral-postman-env.json");

        try {
            JsonObject envJson;
            try (FileReader fr = new FileReader(environmentTemplate)) {
                envJson = JsonParser.parseReader(fr).getAsJsonObject();
            }

            JsonArray values = envJson.getAsJsonArray("values");
            for (JsonElement el : values) {
                JsonObject valObj = el.getAsJsonObject();
                String key = valObj.get("key").getAsString();
                if ("adminToken".equals(key)) {
                    valObj.addProperty("value", adminToken);
                } else if ("userToken".equals(key)) {
                    valObj.addProperty("value", userToken);
                }
            }

            try (FileWriter fw = new FileWriter(ephemeralEnvFile)) {
                fw.write(envJson.toString());
            }

            List<String> command = new ArrayList<>();
            command.add("cmd.exe");
            command.add("/c");
            command.add("npx");
            command.add("-y");
            command.add("newman");
            command.add("run");
            command.add(collectionFile.getAbsolutePath());
            command.add("-e");
            command.add(ephemeralEnvFile.getAbsolutePath());
            command.add("--color");
            command.add("off");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workspaceRoot);
            pb.redirectErrorStream(true);
            pb.environment().put("NODE_OPTIONS", "--max-old-space-size=4096");

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Newman finished with exit code: " + exitCode);
            assertEquals(0, exitCode, "Newman run must pass with 0 failures");
        } finally {
            if (ephemeralEnvFile.exists()) {
                ephemeralEnvFile.delete();
                System.out.println("[CLEANUP] Ephemeral environment file purged from target/");
            }
        }
    }
}
