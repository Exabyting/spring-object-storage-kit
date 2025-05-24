package com.exabyting.springosk.integration;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import com.exabyting.springosk.config.PropertiesConfig;
import com.exabyting.springosk.config.S3Config;
import com.exabyting.springosk.properties.OskProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(classes = {S3Config.class, PropertiesConfig.class}) // Ensure S3Config and OskProperties are loaded
@DisplayName("S3 Integration Tests with Testcontainers")
@Slf4j
class S3IntegrationTest {

    private static final String TEST_BUCKET_NAME = "test-bucket-" + System.currentTimeMillis();
    private static final String TEST_OBJECT_KEY = "test-object.txt";
    private static final String TEST_OBJECT_CONTENT = "Hello Testcontainers S3!";

    @Container
    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:latest"))
                    .withServices(LocalStackContainer.Service.S3)
                    .withEnv("LS_LOG", "trace-internal"); // For detailed localstack logs if needed

    @Autowired
    private S3Client s3Client;
    // Autowire to verify properties are set

    @Autowired
    private OskProperties oskProperties;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("object-storage-kit.storage-type", () -> "s3");
        registry.add("object-storage-kit.endpoint", () -> localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        registry.add("object-storage-kit.region", localStack::getRegion);
        registry.add("object-storage-kit.access-key", localStack::getAccessKey);
        registry.add("object-storage-kit.secret-key", localStack::getSecretKey);
        registry.add("object-storage-kit.path-style-access", () -> true); // Important for LocalStack
        registry.add("object-storage-kit.default-bucket", () -> TEST_BUCKET_NAME); // Set default bucket for testing
        registry.add("object-storage-kit.auto-create-bucket", () -> false); // Disable auto-creation for this test
    }

    @BeforeAll
    static void beforeAll() {
        // Testcontainers will start the container automatically.
        // You can add a check here if needed, but @Container handles it.
        assertTrue(localStack.isRunning(), "LocalStack container should be running.");
    }
    
    @BeforeEach
    void setUp() {
        // Ensure the S3 client is available
        assertNotNull(s3Client, "S3Client should be autowired and configured.");
        assertNotNull(oskProperties, "OskProperties should be autowired.");

        // Verify properties are correctly set from DynamicPropertySource
        assertEquals(localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString(), oskProperties.getEndpoint());
        assertEquals(localStack.getRegion(), oskProperties.getRegion());
        assertEquals(localStack.getAccessKey(), oskProperties.getAccessKey());
        assertEquals(localStack.getSecretKey(), oskProperties.getSecretKey());
        assertTrue(oskProperties.getPathStyleAccess(), "Path style access should be true for LocalStack.");
        assertEquals(TEST_BUCKET_NAME, oskProperties.getDefaultBucket(), "Default bucket in properties should match test bucket.");


        // Create the bucket for each test method to ensure isolation
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(TEST_BUCKET_NAME).build());
            // Wait for bucket to be created - LocalStack can be a bit slow
            s3Client.waiter().waitUntilBucketExists(HeadBucketRequest.builder().bucket(TEST_BUCKET_NAME).build());
        } catch (Exception e) {
            // Handle cases where bucket might already exist from a previous failed run if not cleaned up
            // Or if there's an issue with creation.
            System.err.println("Error creating bucket in setUp: " + e.getMessage());
            // Depending on the desired behavior, you might re-throw or fail the test here.
        }
    }


    @Test
    @DisplayName("Should upload an object to S3")
    void shouldUploadObject() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString(TEST_OBJECT_CONTENT, StandardCharsets.UTF_8));

        // Verify by trying to get the object
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        String retrievedContent = objectBytes.asString(StandardCharsets.UTF_8);
        assertEquals(TEST_OBJECT_CONTENT, retrievedContent, "Retrieved object content should match uploaded content.");
    }

    @Test
    @DisplayName("Should download an object from S3")
    void shouldDownloadObject() throws IOException {
        // First, upload an object
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString(TEST_OBJECT_CONTENT, StandardCharsets.UTF_8));

        // Now, download it
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        String content = objectBytes.asString(StandardCharsets.UTF_8);

        assertEquals(TEST_OBJECT_CONTENT, content, "Downloaded content should match the original content.");
    }

    @Test
    @DisplayName("Should delete an object from S3")
    void shouldDeleteObject() {
        // Upload an object first
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromString(TEST_OBJECT_CONTENT, StandardCharsets.UTF_8));

        // Delete the object
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        s3Client.deleteObject(deleteObjectRequest);

        // Verify object is deleted (trying to get it should fail or return not found)
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(TEST_BUCKET_NAME)
                .key(TEST_OBJECT_KEY)
                .build();
        assertThrows(Exception.class, () -> { // Expecting SdkException or specific NoSuchKeyException
            s3Client.getObject(getObjectRequest);
        }, "Getting a deleted object should throw an exception.");
    }

    @AfterEach
    void tearDown() {
        if (s3Client == null) {
            log.warn("S3Client is null in tearDown, skipping S3 cleanup for bucket: {}", TEST_BUCKET_NAME);
            return;
        }
        try {
            log.info("Starting S3 cleanup for bucket: {}", TEST_BUCKET_NAME);

            // List all objects in the bucket
            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .build();
            ListObjectsV2Response listObjectsV2Response;
            List<ObjectIdentifier> objectsToDelete = new ArrayList<>();

            do {
                listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
                for (S3Object s3Object : listObjectsV2Response.contents()) {
                    objectsToDelete.add(ObjectIdentifier.builder().key(s3Object.key()).build());
                }
                String nextToken = listObjectsV2Response.nextContinuationToken();
                listObjectsV2Request = listObjectsV2Request.toBuilder()
                        .continuationToken(nextToken)
                        .build();
            } while (listObjectsV2Response.isTruncated());

            if (!objectsToDelete.isEmpty()) {
                DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                        .bucket(TEST_BUCKET_NAME)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();
                s3Client.deleteObjects(deleteObjectsRequest);
                log.info("Successfully deleted {} objects from bucket: {}", objectsToDelete.size(), TEST_BUCKET_NAME);
            } else {
                log.info("No objects found in bucket {} to delete.", TEST_BUCKET_NAME);
            }

            // Delete the bucket
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .bucket(TEST_BUCKET_NAME)
                    .build();
            s3Client.deleteBucket(deleteBucketRequest);
            log.info("Successfully deleted bucket: {}", TEST_BUCKET_NAME);

        } catch (Exception e) {
            log.error("Error during S3 cleanup for bucket {}: {}. This might be due to the bucket not existing or other S3 issues.",
                    TEST_BUCKET_NAME, e.getMessage(), e);
        }
    }
    
    @AfterAll
    static void afterAll() {
        // Testcontainers will stop the localStack container automatically.
        // Additional static cleanup for the class can be performed here if needed.
        if (localStack != null && localStack.isRunning()) {
            log.info("LocalStack container is still running. Testcontainers will manage its shutdown.");
        } else if (localStack != null) {
            log.info("LocalStack container is not running at @AfterAll.");
        }
        // Bucket cleanup is handled in @AfterEach for instance-specific resources like s3Client.
    }
}
