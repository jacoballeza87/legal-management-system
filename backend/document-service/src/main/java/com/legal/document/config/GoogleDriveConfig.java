package com.legal.document.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.FileInputStream;
import java.util.Collections;

@Configuration
@Slf4j
public class GoogleDriveConfig {

    @Value("${google.drive.credentials-path}")
    private String credentialsPath;

    @Value("${google.drive.enabled:false}")
    private boolean driveEnabled;

    @Bean
    public Drive googleDriveService() {
        if (!driveEnabled) {
            log.warn("Google Drive deshabilitado (google.drive.enabled=false). Retornando null bean.");
            return null;
        }
        try {
            GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singletonList(DriveScopes.DRIVE));

            return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Legal Management System")
                .build();
        } catch (Exception e) {
            log.error("Error configurando Google Drive: {}. Servicios de Drive no disponibles.", e.getMessage());
            return null;
        }
    }
}
