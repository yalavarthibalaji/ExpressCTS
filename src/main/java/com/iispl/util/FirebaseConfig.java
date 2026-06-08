package com.iispl.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.InputStream;

public class FirebaseConfig {

    private static boolean initialised = false;

    public static synchronized void init() {
        if (initialised) return;
        try {
            InputStream serviceAccount = FirebaseConfig.class
                .getClassLoader()
                .getResourceAsStream("firebase-service-account.json");

            // Clear error if file not found
            if (serviceAccount == null) {
                throw new RuntimeException(
                    "firebase-service-account.json NOT FOUND on classpath. " +
                    "Place it under src/main/resources/ and rebuild."
                );
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl("https://cbs-validation-api-default-rtdb.asia-southeast1.firebasedatabase.app")
                .build();

            FirebaseApp.initializeApp(options);
            initialised = true;
            System.out.println("FirebaseConfig → Initialised successfully.");

        } catch (RuntimeException e) {
            throw e; // re-throw as-is
        } catch (Exception e) {
            throw new RuntimeException("Firebase init failed: " + e.getMessage(), e);
        }
    }
}