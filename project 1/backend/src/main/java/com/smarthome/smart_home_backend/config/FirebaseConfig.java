package com.smarthome.smart_home_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        String credPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credPath == null || credPath.trim().isEmpty()) {
            credPath = System.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
        }
        if (credPath == null || credPath.trim().isEmpty()) {
            String defaultPath = "C:\\Users\\akhil\\OneDrive\\Desktop\\firebase-secrets\\smart-home-service-account.json";
            if (new File(defaultPath).exists()) {
                credPath = defaultPath;
            }
        }

        GoogleCredentials credentials;

        if (credPath != null && !credPath.trim().isEmpty()) {
            File credFile = new File(credPath.trim());
            if (credFile.exists()) {
                log.info("Initializing Firebase Admin SDK using service account from GOOGLE_APPLICATION_CREDENTIALS: {}", credFile.getName());
                try (InputStream is = new FileInputStream(credFile)) {
                    credentials = GoogleCredentials.fromStream(is);
                }
            } else {
                log.warn("GOOGLE_APPLICATION_CREDENTIALS path specified ({}) but file does not exist. Falling back to Application Default Credentials.", credFile.getName());
                credentials = GoogleCredentials.getApplicationDefault();
            }
        } else {
            log.info("GOOGLE_APPLICATION_CREDENTIALS not explicitly set. Attempting Application Default Credentials.");
            credentials = GoogleCredentials.getApplicationDefault();
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase Admin SDK initialized successfully: {}", app.getName());
        return app;
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
