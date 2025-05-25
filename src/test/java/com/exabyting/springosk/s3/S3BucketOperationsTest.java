package com.exabyting.springosk.s3;

import com.exabyting.springosk.exception.BucketOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3BucketOperations Unit Tests")
class S3BucketOperationsTest {

    @Mock
    private S3Client s3Client;

    private S3BucketOperations s3BucketOperations;

    private static final String TEST_BUCKET_NAME = "test-bucket";
    private static final String TEST_OBJECT_KEY_1 = "object1.txt";
    private static final String TEST_OBJECT_KEY_2 = "object2.txt";

    @BeforeEach
    void setUp() {
        s3BucketOperations = new S3BucketOperations(s3Client);
    }

    @Test
    @DisplayName("Should create bucket successfully")
    void shouldCreateBucketSuccessfully() {
        // Given
        CreateBucketResponse createBucketResponse = CreateBucketResponse.builder().build();
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenReturn(createBucketResponse);

        // When
        Boolean result = s3BucketOperations.create(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).createBucket(argThat((CreateBucketRequest request) -> 
            request.bucket().equals(TEST_BUCKET_NAME)
        ));
    }

    @Test
    @DisplayName("Should throw exception when S3Exception occurs during bucket creation")
    void shouldThrowExWhenS3ExceptionOccursDuringBucketCreation() {
        // Given
         var s3Exception = (S3Exception) S3Exception.builder()
            .message("Bucket already exists")
            .statusCode(409)
            .build();
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenThrow(s3Exception);

        assertThrows(BucketOperationException.class, () -> {
            // When
            s3BucketOperations.create(TEST_BUCKET_NAME);
        });
    }

    @Test
    @DisplayName("Should throw exception when general exception occurs during bucket creation")
    void shouldThrowExceptionWhenGeneralExceptionOccursDuringBucketCreation() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Network error");
        when(s3Client.createBucket(any(CreateBucketRequest.class))).thenThrow(runtimeException);

        // When & Then
        assertThrows(BucketOperationException.class, () -> s3BucketOperations.create(TEST_BUCKET_NAME));
        verify(s3Client).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    @DisplayName("Should delete bucket successfully when no objects exist")
    void shouldDeleteBucketSuccessfullyWhenNoObjectsExist() {
        // Given
        ListObjectsV2Response emptyListResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of())
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(emptyListResponse);
        
        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client).deleteBucket(argThat((DeleteBucketRequest request) -> 
            request.bucket().equals(TEST_BUCKET_NAME)
        ));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
    }

    @Test
    @DisplayName("Should delete bucket successfully after deleting objects")
    void shouldDeleteBucketSuccessfullyAfterDeletingObjects() {
        // Given
        S3Object object1 = S3Object.builder()
            .key(TEST_OBJECT_KEY_1)
            .lastModified(Instant.now())
            .size(100L)
            .build();
        S3Object object2 = S3Object.builder()
            .key(TEST_OBJECT_KEY_2)
            .lastModified(Instant.now())
            .size(200L)
            .build();

        ListObjectsV2Response listObjectsResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of(object1, object2))
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsResponse);

        DeleteObjectsResponse deleteObjectsResponse = DeleteObjectsResponse.builder().build();
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResponse);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client).deleteObjects(argThat((DeleteObjectsRequest request) -> {
            Delete delete = request.delete();
            List<ObjectIdentifier> objects = delete.objects();
            return objects.size() == 2 &&
                objects.stream().anyMatch(obj -> obj.key().equals(TEST_OBJECT_KEY_1)) &&
                objects.stream().anyMatch(obj -> obj.key().equals(TEST_OBJECT_KEY_2));
        }));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should delete bucket successfully with paginated object listing")
    void shouldDeleteBucketSuccessfullyWithPaginatedObjectListing() {
        // Given
        S3Object object1 = S3Object.builder()
            .key(TEST_OBJECT_KEY_1)
            .lastModified(Instant.now())
            .size(100L)
            .build();
        S3Object object2 = S3Object.builder()
            .key(TEST_OBJECT_KEY_2)
            .lastModified(Instant.now())
            .size(200L)
            .build();

        // First page response
        ListObjectsV2Response firstPageResponse = ListObjectsV2Response.builder()
            .isTruncated(true)
            .nextContinuationToken("token123")
            .contents(List.of(object1))
            .build();

        // Second page response  
        ListObjectsV2Response secondPageResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of(object2))
            .build();

        when(s3Client.listObjectsV2(argThat((ListObjectsV2Request request) -> 
            request != null && request.continuationToken() == null
        ))).thenReturn(firstPageResponse);

        when(s3Client.listObjectsV2(argThat((ListObjectsV2Request request) -> 
            request != null && "token123".equals(request.continuationToken())
        ))).thenReturn(secondPageResponse);

        DeleteObjectsResponse deleteObjectsResponse = DeleteObjectsResponse.builder().build();
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(deleteObjectsResponse);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client).deleteObjects(argThat((DeleteObjectsRequest request) -> {
            Delete delete = request.delete();
            List<ObjectIdentifier> objects = delete.objects();
            return objects.size() == 2 &&
                objects.stream().anyMatch(obj -> obj.key().equals(TEST_OBJECT_KEY_1)) &&
                objects.stream().anyMatch(obj -> obj.key().equals(TEST_OBJECT_KEY_2));
        }));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when S3Exception occurs during bucket deletion")
    void shouldThrowExceptionWhenS3ExceptionOccursDuringBucketDeletion() {
        // Given
        ListObjectsV2Response emptyListResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of())
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(emptyListResponse);

        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Bucket not found")
            .statusCode(404)
            .build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenThrow(s3Exception);

        // When & Then
        assertThrows(BucketOperationException.class, () -> s3BucketOperations.delete(TEST_BUCKET_NAME));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when general exception occurs during bucket deletion")
    void shouldThrowExceptionWhenGeneralExceptionOccursDuringBucketDeletion() {
        // Given
        ListObjectsV2Response emptyListResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of())
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(emptyListResponse);

        RuntimeException runtimeException = new RuntimeException("Network error");
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenThrow(runtimeException);

        // When & Then
        assertThrows(BucketOperationException.class, () -> s3BucketOperations.delete(TEST_BUCKET_NAME));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should continue bucket deletion even when object deletion fails with S3Exception")
    void shouldContinueBucketDeletionEvenWhenObjectDeletionFailsWithS3Exception() {
        // Given
        S3Object object1 = S3Object.builder()
            .key(TEST_OBJECT_KEY_1)
            .lastModified(Instant.now())
            .size(100L)
            .build();

        ListObjectsV2Response listObjectsResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of(object1))
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsResponse);

        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied for object deletion")
            .statusCode(403)
            .build();
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenThrow(s3Exception);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should continue bucket deletion even when object deletion fails with general exception")
    void shouldContinueBucketDeletionEvenWhenObjectDeletionFailsWithGeneralException() {
        // Given
        S3Object object1 = S3Object.builder()
            .key(TEST_OBJECT_KEY_1)
            .lastModified(Instant.now())
            .size(100L)
            .build();

        ListObjectsV2Response listObjectsResponse = ListObjectsV2Response.builder()
            .isTruncated(false)
            .contents(List.of(object1))
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listObjectsResponse);

        RuntimeException runtimeException = new RuntimeException("Network timeout");
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenThrow(runtimeException);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should continue bucket deletion even when object listing fails with S3Exception")
    void shouldContinueBucketDeletionEvenWhenObjectListingFailsWithS3Exception() {
        // Given
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied for listing objects")
            .statusCode(403)
            .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(s3Exception);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should continue bucket deletion even when object listing fails with general exception")
    void shouldContinueBucketDeletionEvenWhenObjectListingFailsWithGeneralException() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Connection timeout");
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenThrow(runtimeException);

        DeleteBucketResponse deleteBucketResponse = DeleteBucketResponse.builder().build();
        when(s3Client.deleteBucket(any(DeleteBucketRequest.class))).thenReturn(deleteBucketResponse);

        // When
        Boolean result = s3BucketOperations.delete(TEST_BUCKET_NAME);

        // Then
        assertTrue(result);
        verify(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
        verify(s3Client, never()).deleteObjects(any(DeleteObjectsRequest.class));
        verify(s3Client).deleteBucket(any(DeleteBucketRequest.class));
    }

    @Test
    @DisplayName("Should get all buckets successfully")
    void shouldGetAllBucketsSuccessfully() {
        // Given
        Bucket bucket1 = Bucket.builder()
            .name("bucket1")
            .creationDate(Instant.now())
            .build();
        Bucket bucket2 = Bucket.builder()
            .name("bucket2")
            .creationDate(Instant.now())
            .build();

        ListBucketsResponse listBucketsResponse = ListBucketsResponse.builder()
            .buckets(List.of(bucket1, bucket2))
            .build();
        when(s3Client.listBuckets(any(ListBucketsRequest.class))).thenReturn(listBucketsResponse);

        // When
        Collection<String> result = s3BucketOperations.getAllBuckets();

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("bucket1"));
        assertTrue(result.contains("bucket2"));
        verify(s3Client).listBuckets(any(ListBucketsRequest.class));
    }

    @Test
    @DisplayName("Should return empty list when no buckets exist")
    void shouldReturnEmptyListWhenNoBucketsExist() {
        // Given
        ListBucketsResponse listBucketsResponse = ListBucketsResponse.builder()
            .buckets(List.of())
            .build();
        when(s3Client.listBuckets(any(ListBucketsRequest.class))).thenReturn(listBucketsResponse);

        // When
        Collection<String> result = s3BucketOperations.getAllBuckets();

        // Then
        assertTrue(result.isEmpty());
        verify(s3Client).listBuckets(any(ListBucketsRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when S3Exception occurs during bucket listing")
    void shouldThrowExceptionWhenS3ExceptionOccursDuringBucketListing() {
        // Given
        S3Exception s3Exception = (S3Exception) S3Exception.builder()
            .message("Access denied")
            .statusCode(403)
            .build();
        when(s3Client.listBuckets(any(ListBucketsRequest.class))).thenThrow(s3Exception);

        // When & Then
        assertThrows(BucketOperationException.class, () -> s3BucketOperations.getAllBuckets());
        verify(s3Client).listBuckets(any(ListBucketsRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when general exception occurs during bucket listing")
    void shouldThrowExceptionWhenGeneralExceptionOccursDuringBucketListing() {
        // Given
        RuntimeException runtimeException = new RuntimeException("Network error");
        when(s3Client.listBuckets(any(ListBucketsRequest.class))).thenThrow(runtimeException);

        // When & Then
        assertThrows(BucketOperationException.class, () -> s3BucketOperations.getAllBuckets());
        verify(s3Client).listBuckets(any(ListBucketsRequest.class));
    }

    @Test
    @DisplayName("Should handle null bucket name gracefully in create operation")
    void shouldHandleNullBucketNameGracefullyInCreateOperation() {
        // When & Then
        Boolean result = s3BucketOperations.create(null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle null bucket name gracefully in delete operation")
    void shouldHandleNullBucketNameGracefullyInDeleteOperation() {
        // When & Then
        Boolean result = s3BucketOperations.delete(null);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty bucket name gracefully in create operation")
    void shouldHandleEmptyBucketNameGracefullyInCreateOperation() {
        // When & Then
        Boolean result = s3BucketOperations.create("");
        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty bucket name gracefully in delete operation")
    void shouldHandleEmptyBucketNameGracefullyInDeleteOperation() {
        // When & Then
        Boolean result = s3BucketOperations.delete("");
        assertFalse(result);
    }
}
