package com.exabyting.springosk.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;
import com.exabyting.springosk.properties.OskProperties;
import com.exabyting.springosk.properties.StorageType;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {S3Config.class, S3ConfigTest.TestConfig.class})
class S3ConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OskProperties oskProperties() {
            OskProperties properties = new OskProperties();
            properties.setStorageType(StorageType.s3);
            properties.setEndpoint("http://localhost:4566");
            properties.setRegion("us-east-1");
            properties.setAccessKey("test");
            properties.setSecretKey("test");
            properties.setDefaultBucket("test-bucket");
            return properties;
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void s3ClientBeanShouldBeCreated() {
        S3Client s3Client = applicationContext.getBean(S3Client.class);
        assertNotNull(s3Client, "S3Client bean should not be null");
    }
}
