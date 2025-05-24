package com.exabyting.springosk.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import com.exabyting.springosk.properties.OskProperties;
import com.exabyting.springosk.properties.StorageType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    classes = {MinioConfig.class, MinioConfigTest.TestConfig.class},
    properties = {"object-storage-kit.storage-type=minio"}
)
class MinioConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public OskProperties oskProperties() {
            OskProperties properties = new OskProperties();
            properties.setStorageType(StorageType.minio);
            properties.setEndpoint("http://localhost:9000");
            properties.setRegion("us-east-1");
            properties.setAccessKey("minioadmin");
            properties.setSecretKey("minioadmin");
            properties.setDefaultBucket("test-bucket");
            return properties;
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void minioClientBeanShouldBeCreated() {
        Object minioClient = applicationContext.getBean("minioClient");
        assertNotNull(minioClient, "MinioClient bean should not be null");
        
        // Verify it's actually a MinioClient instance
        assertEquals("io.minio.MinioClient", minioClient.getClass().getName(), 
                "Bean should be an instance of MinioClient");
    }
}
