package com.exabyting.springosk.validation;

import com.exabyting.springosk.config.PropertiesConfig;
import com.exabyting.springosk.config.S3Config;
import com.exabyting.springosk.core.ObjectStorageOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
        S3Config.class,
        PropertiesConfig.class,
        ObjectStorageOperations.class,
        com.exabyting.springosk.s3.S3BucketOperations.class,
        com.exabyting.springosk.s3.S3ObjectOperations.class
})
@TestPropertySource(properties = {
        "object-storage-kit.storage-type=s3",
        "object-storage-kit.region=us-east-1",
        "object-storage-kit.access-key=test",
        "object-storage-kit.secret-key=test"
})
class ObjectStorageOperationsValidationTest {

    @Autowired
    private ObjectStorageOperations objectStorageOperations;

    @Test
    void testObjectStorageOperationsAutowiring() {
        assertNotNull(objectStorageOperations, "ObjectStorageOperations should be autowired successfully");
    }
}
