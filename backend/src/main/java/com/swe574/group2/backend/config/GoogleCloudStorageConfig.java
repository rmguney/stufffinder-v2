package com.swe574.group2.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class GoogleCloudStorageConfig {
    
    @Value("${gcp.project-id}")
    private String projectId;
    
    @Value("${gcp.credentials-path:classpath:gcp-credentials.json}")
    private String credentialsPath;
    
    @Bean
    public Storage googleCloudStorage() throws IOException {
        // If using credentials file stored in resources
        GoogleCredentials credentials;
        
        if (credentialsPath.startsWith("classpath:")) {
            // Load from classpath resource
            String resourcePath = credentialsPath.substring("classpath:".length());
            credentials = GoogleCredentials.fromStream(
                new ClassPathResource(resourcePath).getInputStream());
        } else {
            // Alternative approach: use credentials stored in environment variables
            credentials = GoogleCredentials.getApplicationDefault();
        }
        
        return StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(credentials)
                .build()
                .getService();
    }
}