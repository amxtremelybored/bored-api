package in.bored.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Profile("local") // Only active for "local" profile
@Slf4j
public class FirebaseAdminLocalConfig {

    private static final String LOCAL_FIREBASE_CREDENTIAL_PATH = "src/main/resources/firebase-service-account.json";

    @PostConstruct
    public void init() {
        try (FileInputStream serviceAccount = new FileInputStream(LOCAL_FIREBASE_CREDENTIAL_PATH)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin initialized for LOCAL from: {}", LOCAL_FIREBASE_CREDENTIAL_PATH);
            }
        } catch (IOException e) {
            log.error("❌ Firebase LOCAL init failed: {}", e.getMessage());
        }
    }
}