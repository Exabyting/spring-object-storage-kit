package com.exabyting.springosk.properties;

import com.exabyting.springosk.annotation.ValidEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@ConfigurationProperties(prefix = "object-storage-kit")
@Validated
@Data
public class OskProperties {
    @NotNull
    @ValidEnum(enumClass = StorageType.class)
    private StorageType storageType;

    // Connection settings
    @NotNull
    private String endpoint;
    @NotNull
    private String region;
    private Boolean pathStyleAccess = false;

    // Authentication
    @NotNull
    private String accessKey;
    @NotNull
    private String secretKey;
    private String sessionToken; // Optional

    // Default bucket configuration
    @NotNull
    private String defaultBucket;
    private Boolean autoCreateBucket = false;

    // Connection and timeout settings
    private Integer connectionTimeoutMillis = 10000;
    private Integer socketTimeoutMillis = 50000;

    // Transfer acceleration (S3 only)
    private Boolean accelerateModeEnabled = false;
    private Boolean dualStackEnabled = false;

    // Additional headers and metadata
    private String userAgentPrefix;
    private String userAgentSuffix;
}
