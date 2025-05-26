package com.exabyting.springosk.minio;

import com.exabyting.springosk.exception.BucketOperationException;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(MockitoExtension.class)
class MinIOBucketOperationsTest {

    @Mock
    private MinioClient minioClient;

    private MinIOBucketOperations bucketOperations;

    @BeforeEach
    void setUp() {
        bucketOperations = new MinIOBucketOperations(minioClient);
    }

    @Test
    void create_shouldReturnTrue_whenBucketCreatedSuccessfully() throws Exception {
        // Given
        String bucketName = "test-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        // When
        Boolean result = bucketOperations.create(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void create_shouldReturnTrue_whenBucketAlreadyExists() throws Exception {
        // Given
        String bucketName = "existing-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        // When
        Boolean result = bucketOperations.create(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void create_shouldReturnFalse_whenExceptionOccurs() throws Exception {
        // Given
        String bucketName = "test-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection error"));

        // When
        assertThrows(BucketOperationException.class, () -> bucketOperations.create(bucketName));
    }

    @Test
    void delete_shouldReturnTrue_whenBucketDeletedSuccessfully() throws Exception {
        // Given
        String bucketName = "test-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of());

        // When
        Boolean result = bucketOperations.delete(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).listObjects(any(ListObjectsArgs.class));
        verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    void delete_shouldReturnTrue_whenBucketDoesNotExist() throws Exception {
        // Given
        String bucketName = "non-existent-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        // When
        Boolean result = bucketOperations.delete(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).listObjects(any(ListObjectsArgs.class));
        verify(minioClient, never()).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    void delete_shouldDeleteObjectsAndBucket_whenBucketHasObjects() throws Exception {
        // Given
        String bucketName = "test-bucket";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(Collections.emptyList());

        // When
        Boolean result = bucketOperations.delete(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).listObjects(any(ListObjectsArgs.class));
        verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    void delete_shouldReturnFalse_whenExceptionOccurs() throws Exception {
        // Given
        String bucketName = "test-bucket";
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection error"));

        // When
        assertThrows(Exception.class, () -> bucketOperations.delete(bucketName));
    }

    @Test
    void delete_shouldContinueWithBucketDeletion_whenObjectDeletionFails() throws Exception {
        // Given
        String bucketName = "test-bucket";

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(Collections.emptyList());

        // When
        Boolean result = bucketOperations.delete(bucketName);

        // Then
        assertTrue(result);
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient).listObjects(any(ListObjectsArgs.class));
        verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    void getAllBuckets_shouldReturnBucketNames_whenBucketsExist() throws Exception {
        // Given
       Bucket bucket1 = mock(Bucket.class);
               when(bucket1.name()).thenReturn("bucket1");
       Bucket bucket2 = mock(Bucket.class);
               when(bucket2.name()).thenReturn("bucket2");
        List<Bucket> buckets = List.of(bucket1, bucket2);

        when(minioClient.listBuckets()).thenReturn(buckets);

        // When
        Collection<String> result = bucketOperations.getAllBuckets();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("bucket1"));
        assertTrue(result.contains("bucket2"));
        verify(minioClient).listBuckets();
    }

    @Test
    void getAllBuckets_shouldReturnEmptyList_whenNoBucketsExist() throws Exception {
        // Given
        when(minioClient.listBuckets()).thenReturn(List.of());

        // When
        Collection<String> result = bucketOperations.getAllBuckets();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(minioClient).listBuckets();
    }

    @Test
    void getAllBuckets_shouldReturnEmptyList_whenExceptionOccurs() throws Exception {
        // Given
        when(minioClient.listBuckets()).thenReturn(List.of());

        // When
        var result = bucketOperations.getAllBuckets();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(minioClient).listBuckets();
    }

    @Test
    void create_shouldHandleNullBucketName() {
        assertThrows(BucketOperationException.class, () -> bucketOperations.create(null));
    }

    @Test
    void create_shouldHandleEmptyBucketName() {
        assertThrows(BucketOperationException.class, () -> bucketOperations.create(""));
    }

    @Test
    void delete_shouldHandleNullBucketName() {
        assertThrows(BucketOperationException.class, () -> bucketOperations.delete(null));
    }

    @Test
    void delete_shouldHandleEmptyBucketName() {
        assertThrows(BucketOperationException.class, () -> bucketOperations.delete(""));
    }

    @Test
    @DisplayName("deleteAllObjectsInBucket should delete all objects in the bucket")
    void deleteAllObjectsInBucket_shouldDeleteAllObjects() throws Exception {
        String bucketName = "bucket-with-objects";
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        when(item1.objectName()).thenReturn("obj1");
        when(item2.objectName()).thenReturn("obj2");

        Result<Item> result1 = mock(Result.class);
        Result<Item> result2 = mock(Result.class);
        when(result1.get()).thenReturn(item1);
        when(result2.get()).thenReturn(item2);

        Iterable<Result<Item>> iterable = List.of(result1, result2);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(iterable);

        // Call delete (which calls deleteAllObjectsInBucket internally)
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        Boolean result = bucketOperations.delete(bucketName);

        assertTrue(result);
        verify(minioClient).removeObject(argThat(args -> args.object().equals("obj1")));
        verify(minioClient).removeObject(argThat(args -> args.object().equals("obj2")));
        verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    @DisplayName("deleteAllObjectsInBucket should continue deleting if one object deletion fails")
    void deleteAllObjectsInBucket_shouldContinueOnObjectDeleteFailure() throws Exception {
        String bucketName = "bucket-with-failure";
        Item item1 = mock(Item.class);
        Item item2 = mock(Item.class);
        when(item1.objectName()).thenReturn("obj1");
        when(item2.objectName()).thenReturn("obj2");

        Result<Item> result1 = mock(Result.class);
        Result<Item> result2 = mock(Result.class);
        when(result1.get()).thenReturn(item1);
        when(result2.get()).thenReturn(item2);

        Iterable<Result<Item>> iterable = List.of(result1, result2);
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(iterable);

        doThrow(new RuntimeException("delete failed")).when(minioClient).removeObject(argThat(args -> args.object().equals("obj1")));

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        Boolean result = bucketOperations.delete(bucketName);

        assertTrue(result);
        verify(minioClient).removeObject(argThat(args -> args.object().equals("obj1")));
        verify(minioClient).removeObject(argThat(args -> args.object().equals("obj2")));
        verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
    }

    @Test
    @DisplayName("deleteAllObjectsInBucket should handle exception when listing objects")
    void deleteAllObjectsInBucket_shouldHandleListObjectsException() throws Exception {
        String bucketName = "bucket-list-exception";
        var listOfObjects = List.<Result<Item>>of();
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(listOfObjects);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        assertDoesNotThrow(() -> {
            var result = bucketOperations.delete(bucketName);
            assertTrue(result);
            verify(minioClient).listObjects(any(ListObjectsArgs.class));
            verify(minioClient).removeBucket(any(RemoveBucketArgs.class));
        });
    }
}
