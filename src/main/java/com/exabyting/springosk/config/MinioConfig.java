package com.exabyting.springosk.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.exabyting.springosk.annotation.ConditionalOnStorageType;
import com.exabyting.springosk.properties.OskProperties;
import okhttp3.OkHttpClient;
import java.time.Duration;

/**
 * Configuration class for MinIO client bean.
 * This configuration is active when the storage type is set to "minio".
 */
@Configuration
@ConditionalOnStorageType(value = "minio")
@Slf4j
public class MinioConfig {

    /**
     * Creates and configures a MinIO client bean.
     * 
     * @param oskProperties the OSK properties containing MinIO configuration
     * @return configured MinIO client instance
     */
    @Bean
    public io.minio.MinioClient minioClient(OskProperties oskProperties) {
        log.info("Initializing MinioClient with endpoint: {}", oskProperties.getEndpoint());
        
        // Create MinioClient builder
        io.minio.MinioClient.Builder builder = io.minio.MinioClient.builder()
                .endpoint(oskProperties.getEndpoint())
                .httpClient(buildHttpClient(oskProperties));

        // Set region if provided
        if (oskProperties.getRegion() != null && !oskProperties.getRegion().isEmpty()) {
            builder.region(oskProperties.getRegion());
        }

        // Set credentials if provided
        if (oskProperties.getAccessKey() != null && !oskProperties.getAccessKey().isEmpty() &&
            oskProperties.getSecretKey() != null && !oskProperties.getSecretKey().isEmpty()) {
            builder.credentials(oskProperties.getAccessKey(), oskProperties.getSecretKey());
        }

        // Build the client
        io.minio.MinioClient client = builder.build();
        
        log.info("MinioClient successfully initialized for endpoint: {}", oskProperties.getEndpoint());
        return client;
    }

    /**
     * Builds a production-ready HTTP client with timeout and connection settings.
     * 
     * @param oskProperties the OSK properties containing HTTP configuration
     * @return configured OkHttpClient instance
     */
    private OkHttpClient buildHttpClient(OskProperties oskProperties) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        
        // Set connection timeout
        if (oskProperties.getConnectionTimeoutMillis() != null) {
            httpClientBuilder.connectTimeout(Duration.ofMillis(oskProperties.getConnectionTimeoutMillis()));
        }
        
        // Set socket/read timeout
        if (oskProperties.getSocketTimeoutMillis() != null) {
            httpClientBuilder.readTimeout(Duration.ofMillis(oskProperties.getSocketTimeoutMillis()));
            httpClientBuilder.writeTimeout(Duration.ofMillis(oskProperties.getSocketTimeoutMillis()));
        }
        
        // Set call timeout (overall request timeout)
        httpClientBuilder.callTimeout(Duration.ofMillis(
            Math.max(oskProperties.getSocketTimeoutMillis() != null ? oskProperties.getSocketTimeoutMillis() : 50000,
                    oskProperties.getConnectionTimeoutMillis() != null ? oskProperties.getConnectionTimeoutMillis() : 10000)
        ));
        
        // Configure connection pool for better performance
        httpClientBuilder.connectionPool(new okhttp3.ConnectionPool(
            10, // max idle connections
            5, // keep alive duration in minutes
            java.util.concurrent.TimeUnit.MINUTES
        ));
        
        // Enable retries for better reliability
        httpClientBuilder.retryOnConnectionFailure(true);
        
        log.debug("HTTP client configured with connection timeout: {}ms, socket timeout: {}ms", 
                oskProperties.getConnectionTimeoutMillis(), oskProperties.getSocketTimeoutMillis());
        
        return httpClientBuilder.build();
    }
}
