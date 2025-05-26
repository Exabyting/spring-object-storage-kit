package com.exabyting.springosk.integration;

import com.exabyting.springosk.config.MinioConfig;
import com.exabyting.springosk.config.PropertiesConfig;
import com.exabyting.springosk.properties.OskProperties;
import io.minio.*;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(classes = {MinioConfig.class, PropertiesConfig.class}) // Ensure MinioConfig and OskProperties are loaded
@DisplayName("MinIO Integration Tests with Testcontainers")
@Slf4j
class MinioIntegrationTest {

    private static final String TEST_BUCKET_NAME = "test-bucket-" + System.currentTimeMillis();
    private static final String TEST_OBJECT_KEY = "test-object.txt";
    private static final String TEST_OBJECT_CONTENT = "Hello Testcontainers MinIO!";
    
    // MinIO default ports: 9000 for API, 9001 for console
    @Container
    static GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withExposedPorts(9000, 9001)
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withCommand("server", "/data", "--console-address", ":9001");

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private OskProperties oskProperties;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("object-storage-kit.storage-type", () -> "minio");
        registry.add("object-storage-kit.endpoint", () -> "http://localhost:" + minioContainer.getMappedPort(9000));
        registry.add("object-storage-kit.region", () -> "us-east-1");
        registry.add("object-storage-kit.access-key", () -> "minioadmin");
        registry.add("object-storage-kit.secret-key", () -> "minioadmin");
        registry.add("object-storage-kit.path-style-access", () -> true); // Important for MinIO
        registry.add("object-storage-kit.default-bucket", () -> TEST_BUCKET_NAME); // Set default bucket for testing
        registry.add("object-storage-kit.auto-create-bucket", () -> false); // Disable auto-creation for this test
    }

    @BeforeAll
    static void beforeAll() {
        // Testcontainers will start the container automatically.
        // You can add a check here if needed, but @Container handles it.
        assertTrue(minioContainer.isRunning(), "MinIO container should be running.");
        log.info("MinIO container started with API endpoint: http://localhost:{}", minioContainer.getMappedPort(9000));
    }
    
    @BeforeEach
    void setUp() {
        // Ensure the MinIO client is available
        assertNotNull(minioClient, "MinioClient should be autowired and configured.");
        assertNotNull(oskProperties, "OskProperties should be autowired.");

        // Verify properties are correctly set from DynamicPropertySource
        assertEquals("http://localhost:" + minioContainer.getMappedPort(9000), oskProperties.getEndpoint());
        assertEquals("us-east-1", oskProperties.getRegion());
        assertEquals("minioadmin", oskProperties.getAccessKey());
        assertEquals("minioadmin", oskProperties.getSecretKey());
        assertTrue(oskProperties.getPathStyleAccess(), "Path style access should be true for MinIO.");
        assertEquals(TEST_BUCKET_NAME, oskProperties.getDefaultBucket(), "Default bucket in properties should match test bucket.");

        // Create the bucket for each test method to ensure isolation
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(TEST_BUCKET_NAME).build());
            log.info("Created test bucket: {}", TEST_BUCKET_NAME);
        } catch (Exception e) {
            // Handle cases where bucket might already exist from a previous failed run if not cleaned up
            // Or if there's an issue with creation.
            log.warn("Error creating bucket in setUp: {}. This might be because the bucket already exists.", e.getMessage());
            // Depending on the desired behavior, you might re-throw or fail the test here.
        }
    }

    @Test
    @DisplayName("Should upload an object to MinIO")
    void shouldUploadObject() throws Exception {
        // Upload object
        ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_OBJECT_CONTENT.getBytes(StandardCharsets.UTF_8));
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(TEST_BUCKET_NAME)
                .object(TEST_OBJECT_KEY)
                .stream(inputStream, TEST_OBJECT_CONTENT.length(), -1)
                .contentType("text/plain")
                .build()
        );

        // Verify by trying to get the object
        try (InputStream objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .object(TEST_OBJECT_KEY)
                    .build())) {
            
            String retrievedContent = new String(objectStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_OBJECT_CONTENT, retrievedContent, "Retrieved object content should match uploaded content.");
        }
    }

    @Test
    @DisplayName("Should download an object from MinIO")
    void shouldDownloadObject() throws Exception {
        // First, upload an object
        ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_OBJECT_CONTENT.getBytes(StandardCharsets.UTF_8));
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(TEST_BUCKET_NAME)
                .object(TEST_OBJECT_KEY)
                .stream(inputStream, TEST_OBJECT_CONTENT.length(), -1)
                .contentType("text/plain")
                .build()
        );

        // Now, download it
        try (InputStream objectStream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .object(TEST_OBJECT_KEY)
                    .build())) {
            
            String content = new String(objectStream.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_OBJECT_CONTENT, content, "Downloaded content should match the original content.");
        }
    }

    @Test
    @DisplayName("Should delete an object from MinIO")
    void shouldDeleteObject() throws Exception {
        // Upload an object first
        ByteArrayInputStream inputStream = new ByteArrayInputStream(TEST_OBJECT_CONTENT.getBytes(StandardCharsets.UTF_8));
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(TEST_BUCKET_NAME)
                .object(TEST_OBJECT_KEY)
                .stream(inputStream, TEST_OBJECT_CONTENT.length(), -1)
                .contentType("text/plain")
                .build()
        );

        // Delete the object
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(TEST_BUCKET_NAME)
                .object(TEST_OBJECT_KEY)
                .build()
        );

        // Verify object is deleted (trying to get object stat should fail)
        assertThrows(Exception.class, () -> minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(TEST_BUCKET_NAME)
                        .object(TEST_OBJECT_KEY)
                        .build()
        ), "Getting a deleted object stat should throw an exception.");
    }

    @AfterEach
    void tearDown() {
        if (minioClient == null) {
            log.warn("MinioClient is null in tearDown, skipping MinIO cleanup for bucket: {}", TEST_BUCKET_NAME);
            return;
        }
        try {
            log.info("Starting MinIO cleanup for bucket: {}", TEST_BUCKET_NAME);

            // List all objects in the bucket and delete them
            List<String> objectsToDelete = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .build()
            );
            
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    objectsToDelete.add(item.objectName());
                } catch (Exception e) {
                    log.warn("Error processing object in bucket {}: {}", TEST_BUCKET_NAME, e.getMessage());
                }
            }

            // Delete all objects
            for (String objectName : objectsToDelete) {
                try {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(TEST_BUCKET_NAME)
                                    .object(objectName)
                                    .build()
                    );
                } catch (Exception e) {
                    log.warn("Error deleting object {} from bucket {}: {}", objectName, TEST_BUCKET_NAME, e.getMessage());
                }
            }
            
            if (!objectsToDelete.isEmpty()) {
                log.info("Successfully deleted {} objects from bucket: {}", objectsToDelete.size(), TEST_BUCKET_NAME);
            } else {
                log.info("No objects found in bucket {} to delete.", TEST_BUCKET_NAME);
            }

            // Delete the bucket
            minioClient.removeBucket(RemoveBucketArgs.builder().bucket(TEST_BUCKET_NAME).build());
            log.info("Successfully deleted bucket: {}", TEST_BUCKET_NAME);

        } catch (Exception e) {
            log.error("Error during MinIO cleanup for bucket {}: {}. This might be due to the bucket not existing or other MinIO issues.",
                    TEST_BUCKET_NAME, e.getMessage(), e);
        }
    }
    
    @AfterAll
    static void afterAll() {
        // Testcontainers automatically manages container lifecycle with @Container annotation
        // No need to manually stop the container - this can cause shutdown conflicts
        log.info("Test class completed. Testcontainers will automatically shutdown the MinIO container.");
    }
}
