package space.sadman.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import space.sadman.annotation.ValidEnum;


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

    // Multipart settings
    private Long multipartMinPartSize = 5L * 1024 * 1024; // 5MB
    private Long multipartCopyThreshold = 5L * 1024 * 1024 * 1024; // 5GB
    private Long multipartCopyPartSize = 100L * 1024 * 1024; // 100MB
    private Long minimumUploadPartSize = 5L * 1024 * 1024; // 5MB

    // Transfer acceleration (S3 only)
    private Boolean accelerateModeEnabled = false;
    private Boolean dualStackEnabled = false;

    // Checksum settings
    private Boolean chunkedEncodingDisabled = false;
    private Boolean payloadSigningEnabled = true;

    // Additional headers and metadata
    private String userAgentPrefix;
    private String userAgentSuffix;

    // Logging and debugging
    private Boolean wireLoggingEnabled = false;

    // Cache settings
    private Integer dnsResolverCacheSize = 256;
}
