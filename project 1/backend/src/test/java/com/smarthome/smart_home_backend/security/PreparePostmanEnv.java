package com.smarthome.smart_home_backend.security;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PreparePostmanEnv {

    private static final String FIREBASE_API_KEY = "AIzaSyD3o04kVsWLv6bNgrKMdf-1r878hfbU7WI";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

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
        if (response.statusCode() != 200) {
            throw new RuntimeException("Token exchange failed: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("idToken").getAsString();
    }

    @org.junit.jupiter.api.Test
    public void generateEnvironment() throws Exception {
        main(new String[0]);
    }

    public static void main(String[] args) throws Exception {
        File credFile = new File("C:\\Users\\akhil\\OneDrive\\Desktop\\firebase-secrets\\smart-home-service-account.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credFile));
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        String adminUid = "6bITkR1RdRgzCG1lw0C6nIQAan83";
        String userUid = "n1PL4z3BhAdWp1PT5XxJKuIuvUm1";

        String adminToken = getRealFirebaseIdToken(adminUid);
        String userToken = getRealFirebaseIdToken(userUid);

        File workspaceRoot = new File("c:\\Users\\akhil\\OneDrive\\Desktop\\Projects\\dbms");
        File templateFile = new File(workspaceRoot, "docs\\postman\\SmartHome-Local.postman_environment.json");
        File targetDir = new File(workspaceRoot, "backend\\target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File targetFile = new File(targetDir, "ephemeral-env.json");

        JsonObject envJson;
        try (FileReader fr = new FileReader(templateFile)) {
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

        try (FileWriter fw = new FileWriter(targetFile)) {
            fw.write(envJson.toString());
        }

        System.out.println("SUCCESS: Ephemeral environment created at: " + targetFile.getAbsolutePath());
    }
}
