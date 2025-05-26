package com.exabyting.springosk.minio;

import com.exabyting.springosk.annotation.ConditionalOnStorageType;
import com.exabyting.springosk.core.ObjectOperations;
import com.exabyting.springosk.exception.ObjectOperationException;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@ConditionalOnStorageType(value = "minio")
@RequiredArgsConstructor
@Slf4j
public class MinIOObjectOperations implements ObjectOperations {

    private final MinioClient minioClient;

    @Override
    public Boolean upload(@Nonnull String bucketName, @Nonnull String objectName, @Nonnull byte[] data) {
        try {
            validateParameters(bucketName, objectName);
            if (data == null || data.length == 0) {
                throw new IllegalArgumentException("Data cannot be null");
            }

            log.info("Uploading object '{}' to MinIO bucket '{}'", objectName, bucketName);

            try (InputStream inputStream = new ByteArrayInputStream(data)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, data.length, -1)
                                .build()
                );
            }

            log.info("Successfully uploaded object '{}' to MinIO bucket '{}'", objectName, bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to upload object '{}' to MinIO bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to upload object to MinIO: " + objectName, e);
        }
    }

    @Override
    public byte[] download(@Nonnull String bucketName, @Nonnull String objectName) {
        try {
            validateParameters(bucketName, objectName);

            log.info("Downloading object '{}' from MinIO bucket '{}'", objectName, bucketName);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            )) {
                byte[] data = stream.readAllBytes();
                log.info("Successfully downloaded object '{}' from MinIO bucket '{}', size: {} bytes", objectName, bucketName, data.length);
                return data;
            }
        } catch (Exception e) {
            // Check if object not found
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                log.warn("Object '{}' not found in MinIO bucket '{}'", objectName, bucketName);
                return null;
            }
            log.error("Failed to download object '{}' from MinIO bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to download object from MinIO: " + objectName, e);
        }
    }

    @Override
    public Boolean delete(@Nonnull String bucketName, @Nonnull String objectName) {
        try {
            validateParameters(bucketName, objectName);

            log.info("Deleting object '{}' from MinIO bucket '{}'", objectName, bucketName);

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("Successfully deleted object '{}' from MinIO bucket '{}'", objectName, bucketName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete object '{}' from MinIO bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to delete object from MinIO: " + objectName, e);
        }
    }

    @Override
    public Collection<String> list(@Nonnull String bucketName) {
        try {
            validateBucketName(bucketName);

            log.info("Listing objects in MinIO bucket '{}'", bucketName);

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            List<String> objectNames = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                objectNames.add(item.objectName());
            }

            log.info("Successfully listed {} objects in MinIO bucket '{}'", objectNames.size(), bucketName);
            return objectNames;
        } catch (Exception e) {
            log.error("Failed to list objects in MinIO bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to list objects in MinIO bucket: " + bucketName, e);
        }
    }

    private void validateParameters(String bucketName, String objectName) {
        validateBucketName(bucketName);
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }
    }

    private void validateBucketName(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
    }
}
