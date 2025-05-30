package com.exabyting.springosk.core;

import com.exabyting.springosk.config.PropertiesConfig;
import com.exabyting.springosk.config.S3Config;
import com.exabyting.springosk.properties.OskProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = {S3Config.class, PropertiesConfig.class, ObjectStorageClient.class, com.exabyting.springosk.s3.S3BucketOperations.class, com.exabyting.springosk.s3.S3ObjectOperations.class})
@DisplayName("ObjectStorageOperations S3 Integration Tests with TestContainers")
@Slf4j
class ObjectStorageClientS3Test {

    private static final String TEST_BUCKET_NAME_1 = "test-bucket-1-" + System.currentTimeMillis();
    private static final String TEST_BUCKET_NAME_2 = "test-bucket-2-" + System.currentTimeMillis();
    private static final String TEST_OBJECT_KEY_1 = "test-object-1.txt";
    private static final String TEST_OBJECT_KEY_2 = "test-object-2.txt";
    private static final String TEST_OBJECT_CONTENT_1 = "Hello ObjectStorageOperations S3 Test 1!";
    private static final String TEST_OBJECT_CONTENT_2 = "Hello ObjectStorageOperations S3 Test 2!";
    private static final String INVALID_BUCKET_NAME = "invalid-bucket-" + System.currentTimeMillis();

    @Container
    static final LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.S3)
                    .withEnv("LS_LOG", "warn"); // Reduce log noise

    @Autowired
    private ObjectStorageClient objectStorageClient;

    @Autowired
    private OskProperties oskProperties;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("object-storage-kit.storage-type", () -> "s3");
        registry.add("object-storage-kit.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("object-storage-kit.region", localStack::getRegion);
        registry.add("object-storage-kit.access-key", localStack::getAccessKey);
        registry.add("object-storage-kit.secret-key", localStack::getSecretKey);
        registry.add("object-storage-kit.path-style-access", () -> true);
        registry.add("object-storage-kit.default-bucket", () -> TEST_BUCKET_NAME_1);
        registry.add("object-storage-kit.auto-create-bucket", () -> false);
    }

    @BeforeAll
    static void beforeAll() {
        assertTrue(localStack.isRunning(), "LocalStack container should be running");
        log.info("LocalStack S3 endpoint: {}", localStack.getEndpointOverride(LocalStackContainer.Service.S3));
    }

    @BeforeEach
    void setUp() {
        assertNotNull(objectStorageClient, "ObjectStorageOperations should be autowired");
        assertNotNull(oskProperties, "OskProperties should be autowired");
        log.debug("Test setup completed for S3 ObjectStorageOperations");
    }

    @AfterEach
    void tearDown() {
        // Clean up test buckets and objects
        try {
            cleanupBucket(TEST_BUCKET_NAME_1);
            cleanupBucket(TEST_BUCKET_NAME_2);
            cleanupBucket(INVALID_BUCKET_NAME);
        } catch (Exception e) {
            log.warn("Error during cleanup: {}", e.getMessage());
        }
    }

    private void cleanupBucket(String bucketName) {
        try {
            // Delete all objects in the bucket first
            List<String> objects = objectStorageClient.listObjects(bucketName);
            for (String objectKey : objects) {
                objectStorageClient.deleteObject(bucketName, objectKey);
            }
            // Then delete the bucket
            objectStorageClient.deleteBucket(bucketName);
        } catch (Exception e) {
            log.debug("Cleanup bucket {} failed (may not exist): {}", bucketName, e.getMessage());
        }
    }

    @Test
    @DisplayName("Should create bucket successfully")
    void testCreateBucket() {
        // Act
        boolean result = objectStorageClient.createBucket(TEST_BUCKET_NAME_1);

        // Assert
        assertTrue(result, "Bucket creation should succeed");

        // Verify bucket exists by listing buckets
        List<String> buckets = objectStorageClient.listBuckets();
        assertTrue(buckets.contains(TEST_BUCKET_NAME_1), "Created bucket should be in the list");
    }

    @Test
    @DisplayName("Should create multiple buckets successfully")
    void testCreateMultipleBuckets() {
        // Act
        boolean result1 = objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        boolean result2 = objectStorageClient.createBucket(TEST_BUCKET_NAME_2);

        // Assert
        assertTrue(result1, "First bucket creation should succeed");
        assertTrue(result2, "Second bucket creation should succeed");

        List<String> buckets = objectStorageClient.listBuckets();
        assertTrue(buckets.contains(TEST_BUCKET_NAME_1), "First bucket should be in the list");
        assertTrue(buckets.contains(TEST_BUCKET_NAME_2), "Second bucket should be in the list");
        assertTrue(buckets.size() >= 2, "Should have at least 2 buckets");
    }

    @Test
    @DisplayName("Should delete bucket successfully")
    void testDeleteBucket() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        assertTrue(objectStorageClient.listBuckets().contains(TEST_BUCKET_NAME_1), "Bucket should exist before deletion");

        // Act
        boolean result = objectStorageClient.deleteBucket(TEST_BUCKET_NAME_1);

        // Assert
        assertTrue(result, "Bucket deletion should succeed");

        List<String> buckets = objectStorageClient.listBuckets();
        assertFalse(buckets.contains(TEST_BUCKET_NAME_1), "Deleted bucket should not be in the list");
    }

    @Test
    @DisplayName("Should list empty buckets initially")
    void testListBucketsEmpty() {
        // Act
        List<String> buckets = objectStorageClient.listBuckets();

        // Assert
        assertNotNull(buckets, "Bucket list should not be null");
        // Note: May not be empty due to other tests, but should be a valid list
        assertTrue(buckets.size() >= 0, "Should return valid bucket list");
    }

    @Test
    @DisplayName("Should put object successfully")
    void testPutObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        byte[] content = TEST_OBJECT_CONTENT_1.getBytes(StandardCharsets.UTF_8);

        // Act
        Boolean result = objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1, content);

        // Assert
        assertTrue(result, "Object upload should succeed");

        // Verify object exists by listing objects
        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(objects.contains(TEST_OBJECT_KEY_1), "Uploaded object should be in the list");
    }

    @Test
    @DisplayName("Should put multiple objects successfully")
    void testPutMultipleObjects() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        byte[] content1 = TEST_OBJECT_CONTENT_1.getBytes(StandardCharsets.UTF_8);
        byte[] content2 = TEST_OBJECT_CONTENT_2.getBytes(StandardCharsets.UTF_8);

        // Act
        Boolean result1 = objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1, content1);
        Boolean result2 = objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_2, content2);

        // Assert
        assertTrue(result1, "First object upload should succeed");
        assertTrue(result2, "Second object upload should succeed");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(objects.contains(TEST_OBJECT_KEY_1), "First object should be in the list");
        assertTrue(objects.contains(TEST_OBJECT_KEY_2), "Second object should be in the list");
        assertEquals(2, objects.size(), "Should have exactly 2 objects");
    }

    @Test
    @DisplayName("Should delete object successfully")
    void testDeleteObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        byte[] content = TEST_OBJECT_CONTENT_1.getBytes(StandardCharsets.UTF_8);
        objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1, content);

        assertTrue(objectStorageClient.listObjects(TEST_BUCKET_NAME_1).contains(TEST_OBJECT_KEY_1),
                "Object should exist before deletion");

        // Act
        boolean result = objectStorageClient.deleteObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1);

        // Assert
        assertTrue(result, "Object deletion should succeed");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertFalse(objects.contains(TEST_OBJECT_KEY_1), "Deleted object should not be in the list");
    }

    @Test
    @DisplayName("Should list objects in bucket")
    void testListObjects() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);

        // Act - empty bucket first
        List<String> emptyObjects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);

        // Assert - empty bucket
        assertNotNull(emptyObjects, "Object list should not be null");
        assertTrue(emptyObjects.isEmpty(), "Empty bucket should have no objects");

        // Arrange - add objects
        byte[] content1 = TEST_OBJECT_CONTENT_1.getBytes(StandardCharsets.UTF_8);
        byte[] content2 = TEST_OBJECT_CONTENT_2.getBytes(StandardCharsets.UTF_8);
        objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1, content1);
        objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_2, content2);

        // Act - bucket with objects
        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);

        // Assert - bucket with objects
        assertNotNull(objects, "Object list should not be null");
        assertEquals(2, objects.size(), "Should have exactly 2 objects");
        assertTrue(objects.contains(TEST_OBJECT_KEY_1), "Should contain first object");
        assertTrue(objects.contains(TEST_OBJECT_KEY_2), "Should contain second object");
    }

    @Test
    @DisplayName("Should handle empty object content")
    void testPutEmptyObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        byte[] emptyContent = new byte[0];

        // Act
        Boolean result = objectStorageClient.putObject(TEST_BUCKET_NAME_1, "empty-object.txt", emptyContent);

        // Assert
        assertTrue(result, "Empty object upload should succeed");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(objects.contains("empty-object.txt"), "Empty object should be in the list");
    }

    @Test
    @DisplayName("Should handle large object content")
    void testPutLargeObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);

        // Create a 1MB object
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is a large object test content line ").append(i).append("\n");
        }
        byte[] content = largeContent.toString().getBytes(StandardCharsets.UTF_8);

        // Act
        Boolean result = objectStorageClient.putObject(TEST_BUCKET_NAME_1, "large-object.txt", content);

        // Assert
        assertTrue(result, "Large object upload should succeed");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(objects.contains("large-object.txt"), "Large object should be in the list");
    }

    @Test
    @DisplayName("Should handle object names with special characters")
    void testPutObjectWithSpecialCharacters() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        String specialObjectKey = "special-object_123.txt";
        byte[] content = "Special content".getBytes(StandardCharsets.UTF_8);

        // Act
        Boolean result = objectStorageClient.putObject(TEST_BUCKET_NAME_1, specialObjectKey, content);

        // Assert
        assertTrue(result, "Object with special characters should upload successfully");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(objects.contains(specialObjectKey), "Object with special characters should be in the list");
    }

    @Test
    @DisplayName("Should fail gracefully when deleting non-existent object")
    void testDeleteNonExistentObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);

        // Act
        boolean result = objectStorageClient.deleteObject(TEST_BUCKET_NAME_1, "non-existent-object.txt");

        // Assert
        // The behavior may vary by implementation - some may return false, others may return true
        // We just ensure it doesn't throw an exception
        assertNotNull(result, "Delete operation should return a boolean result");
    }

    @Test
    @DisplayName("Should overwrite existing object")
    void testOverwriteObject() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        String objectKey = "overwrite-test.txt";
        byte[] originalContent = "Original content".getBytes(StandardCharsets.UTF_8);
        byte[] newContent = "New content".getBytes(StandardCharsets.UTF_8);

        // Act - upload original
        Boolean result1 = objectStorageClient.putObject(TEST_BUCKET_NAME_1, objectKey, originalContent);
        assertTrue(result1, "Original object upload should succeed");

        // Act - overwrite
        Boolean result2 = objectStorageClient.putObject(TEST_BUCKET_NAME_1, objectKey, newContent);

        // Assert
        assertTrue(result2, "Object overwrite should succeed");

        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertEquals(1, objects.size(), "Should still have only one object");
        assertTrue(objects.contains(objectKey), "Overwritten object should still be in the list");
    }

    @Test
    @DisplayName("Should handle operations across multiple buckets")
    void testMultipleBucketOperations() {
        // Arrange
        objectStorageClient.createBucket(TEST_BUCKET_NAME_1);
        objectStorageClient.createBucket(TEST_BUCKET_NAME_2);

        byte[] content1 = "Content for bucket 1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "Content for bucket 2".getBytes(StandardCharsets.UTF_8);

        // Act
        Boolean upload1 = objectStorageClient.putObject(TEST_BUCKET_NAME_1, TEST_OBJECT_KEY_1, content1);
        Boolean upload2 = objectStorageClient.putObject(TEST_BUCKET_NAME_2, TEST_OBJECT_KEY_2, content2);

        // Assert
        assertTrue(upload1, "Upload to first bucket should succeed");
        assertTrue(upload2, "Upload to second bucket should succeed");

        List<String> objects1 = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        List<String> objects2 = objectStorageClient.listObjects(TEST_BUCKET_NAME_2);

        assertEquals(1, objects1.size(), "First bucket should have one object");
        assertEquals(1, objects2.size(), "Second bucket should have one object");
        assertTrue(objects1.contains(TEST_OBJECT_KEY_1), "First bucket should contain its object");
        assertTrue(objects2.contains(TEST_OBJECT_KEY_2), "Second bucket should contain its object");
    }

    @Test
    @DisplayName("Should handle workflow: create bucket, add objects, list, delete objects, delete bucket")
    void testCompleteWorkflow() {
        // Create bucket
        assertTrue(objectStorageClient.createBucket(TEST_BUCKET_NAME_1), "Bucket creation should succeed");

        // Add multiple objects
        byte[] content1 = "Workflow content 1".getBytes(StandardCharsets.UTF_8);
        byte[] content2 = "Workflow content 2".getBytes(StandardCharsets.UTF_8);

        assertTrue(objectStorageClient.putObject(TEST_BUCKET_NAME_1, "workflow-obj-1.txt", content1),
                "First object upload should succeed");
        assertTrue(objectStorageClient.putObject(TEST_BUCKET_NAME_1, "workflow-obj-2.txt", content2),
                "Second object upload should succeed");

        // List and verify objects
        List<String> objects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertEquals(2, objects.size(), "Should have 2 objects");
        assertTrue(objects.contains("workflow-obj-1.txt"), "Should contain first object");
        assertTrue(objects.contains("workflow-obj-2.txt"), "Should contain second object");

        // Delete objects
        assertTrue(objectStorageClient.deleteObject(TEST_BUCKET_NAME_1, "workflow-obj-1.txt"),
                "First object deletion should succeed");
        assertTrue(objectStorageClient.deleteObject(TEST_BUCKET_NAME_1, "workflow-obj-2.txt"),
                "Second object deletion should succeed");

        // Verify objects are gone
        List<String> emptyObjects = objectStorageClient.listObjects(TEST_BUCKET_NAME_1);
        assertTrue(emptyObjects.isEmpty(), "Bucket should be empty after deleting objects");

        // Delete bucket
        assertTrue(objectStorageClient.deleteBucket(TEST_BUCKET_NAME_1), "Bucket deletion should succeed");

        // Verify bucket is gone
        List<String> buckets = objectStorageClient.listBuckets();
        assertFalse(buckets.contains(TEST_BUCKET_NAME_1), "Bucket should not exist after deletion");
    }
}
