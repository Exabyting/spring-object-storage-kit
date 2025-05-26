package com.exabyting.springosk.minio;

import com.exabyting.springosk.annotation.ConditionalOnStorageType;
import com.exabyting.springosk.core.BucketOperations;
import com.exabyting.springosk.exception.BucketOperationException;
import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveBucketArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnStorageType(value = "minio")
@RequiredArgsConstructor
@Slf4j
public class MinIOBucketOperations implements BucketOperations {

    private final MinioClient minioClient;

    @Override
    public Boolean create(@Nonnull String bucketName) {
        try {
            if(bucketName.isBlank()) {
                throw new IllegalArgumentException("Bucket name cannot be null or empty");
            }

            log.info("Creating MinIO bucket: {}", bucketName);

            // Check if bucket already exists
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (bucketExists) {
                log.info("MinIO bucket '{}' already exists", bucketName);
                return true;
            }

            // Create the bucket
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Successfully created MinIO bucket: {}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to create MinIO bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to create MinIO bucket", e);
        }
    }

    @Override
    public Boolean delete(@Nonnull String bucketName) {

        try {
            if(bucketName.isBlank()) {
                throw new IllegalArgumentException("Bucket name cannot be null or empty");
            }

            log.info("Deleting MinIO bucket: {}", bucketName);

            // Check if bucket exists
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!bucketExists) {
                log.info("MinIO bucket '{}' does not exist", bucketName);
                return true;
            }

            // First, delete all objects in the bucket
            deleteAllObjectsInBucket(bucketName);

            // Then delete the bucket itself
            minioClient.removeBucket(
                    RemoveBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Successfully deleted MinIO bucket: {}", bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete MinIO bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to delete MinIO bucket", e);
        }
    }

    @Override
    public Collection<String> getAllBuckets() {
        try {
            log.debug("Listing all MinIO buckets");

            List<Bucket> buckets = minioClient.listBuckets();

            List<String> bucketNames = buckets.stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());

            log.debug("Found {} MinIO buckets", bucketNames.size());
            return bucketNames;
        } catch (Exception e) {
            log.error("Failed to list MinIO buckets: {}", e.getMessage(), e);
            throw new BucketOperationException("Failed to list MinIO buckets", e);
        }
    }

    private void deleteAllObjectsInBucket(String bucketName) {
        try {
            if(bucketName.isBlank()) {
                throw new IllegalArgumentException("Bucket name cannot be null or empty");
            }

            log.debug("Deleting all objects in bucket: {}", bucketName);

            // List all objects in the bucket
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            int deletedCount = 0;
            int failedCount = 0;
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();

                    // Delete each object
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(item.objectName())
                                    .build()
                    );
                    deletedCount++;
                } catch (Exception e) {
                    failedCount++;
                    log.warn("Failed to delete object from bucket '{}': {}", bucketName, e.getMessage());
                    // Continue with next object
                }
            }

            if (deletedCount > 0) {
                log.debug("Successfully deleted {} objects from bucket: {}", deletedCount, bucketName);
            } else {
                log.debug("No objects found in bucket {} to delete", bucketName);
            }

            if (failedCount > 0) {
                log.warn("Failed to delete {} objects from bucket: {}", failedCount, bucketName);
            }
        } catch (Exception e) {
            log.error("Failed to delete objects in bucket '{}': {}", bucketName, e.getMessage());
            throw new BucketOperationException("Failed to delete objects in MinIO bucket", e);
        }
    }
}
