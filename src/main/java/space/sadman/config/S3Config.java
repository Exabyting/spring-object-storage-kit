package space.sadman.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import space.sadman.annotation.ConditionalOnStorageType;
import space.sadman.properties.OskProperties;

import java.net.URI;
import java.time.Duration;

@Configuration
@ConditionalOnStorageType
@Slf4j
public class S3Config {

    @Bean
    public S3Client s3Client(OskProperties oskProperties) {
        var builder = S3Client.builder()
                .region(Region.of(oskProperties.getRegion()))
                .serviceConfiguration(buildS3Configuration(oskProperties))
                .httpClient(buildHttpClient(oskProperties))
                .overrideConfiguration(buildOverrideConfiguration(oskProperties));

        if (oskProperties.getEndpoint() != null && !oskProperties.getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(oskProperties.getEndpoint()));
        }
        var credentialsProvider = buildCredentialsProvider(oskProperties);
        if (credentialsProvider != null) {
            builder.credentialsProvider(credentialsProvider);
        }
        return builder.build();
    }

    private S3Configuration buildS3Configuration(OskProperties oskProperties) {
        S3Configuration.Builder s3ConfigBuilder = S3Configuration.builder();
        if (Boolean.TRUE.equals(oskProperties.getPathStyleAccess())) {
            s3ConfigBuilder.pathStyleAccessEnabled(true);
        }
        if (Boolean.TRUE.equals(oskProperties.getAccelerateModeEnabled())) {
            s3ConfigBuilder.accelerateModeEnabled(true);
        }
        if (Boolean.TRUE.equals(oskProperties.getDualStackEnabled())){
            s3ConfigBuilder.dualstackEnabled();
        }
        return s3ConfigBuilder.build();
    }

    private SdkHttpClient buildHttpClient(OskProperties oskProperties) {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (oskProperties.getSocketTimeoutMillis() != null) {
            httpClientBuilder.socketTimeout(Duration.ofMillis(oskProperties.getSocketTimeoutMillis()));
        }
        if (oskProperties.getConnectionTimeoutMillis() != null) {
            httpClientBuilder.connectionTimeout(Duration.ofMillis(oskProperties.getConnectionTimeoutMillis()));
        }
        return httpClientBuilder.build();
    }

    private ClientOverrideConfiguration buildOverrideConfiguration(OskProperties oskProperties) {
        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();
        if (oskProperties.getUserAgentPrefix() != null && !oskProperties.getUserAgentPrefix().isEmpty()) {
            builder.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, oskProperties.getUserAgentPrefix());
        }
        if (oskProperties.getUserAgentSuffix() != null && !oskProperties.getUserAgentSuffix().isEmpty()) {
            builder.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, oskProperties.getUserAgentSuffix());
        }
        return builder.build();
    }

    private StaticCredentialsProvider buildCredentialsProvider(OskProperties oskProperties) {
        String accessKey = oskProperties.getAccessKey();
        String secretKey = oskProperties.getSecretKey();
        String sessionToken = oskProperties.getSessionToken();
        if (accessKey == null || accessKey.isEmpty() || secretKey == null || secretKey.isEmpty()) {
            return null;
        }
        if (sessionToken != null && !sessionToken.isEmpty()) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
            );
        }
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }
}
