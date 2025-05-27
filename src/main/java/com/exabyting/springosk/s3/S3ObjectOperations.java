package com.exabyting.springosk.s3;

import com.exabyting.springosk.annotation.ConditionalOnStorageType;
import com.exabyting.springosk.core.ObjectOperations;
import com.exabyting.springosk.exception.ObjectOperationException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnStorageType
public class S3ObjectOperations implements ObjectOperations {

    private final S3Client s3Client;

    @Override
    public Boolean upload(@Nonnull String bucketName, @Nonnull String objectName, @Nonnull byte[] data) {
        try {
            validateParameters(bucketName, objectName);

            log.info("Uploading object '{}' to S3 bucket '{}'", objectName, bucketName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));

            log.info("Successfully uploaded object '{}' to S3 bucket '{}'", objectName, bucketName);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to upload object '{}' to S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to upload object to S3: " + objectName, e);
        } catch (Exception e) {
            log.error("Unexpected error while uploading object '{}' to S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to upload object to S3: " + objectName, e);
        }
    }

    @Override
    public byte[] download(@Nonnull String bucketName, @Nonnull String objectName) {
        try {
            validateParameters(bucketName, objectName);

            log.info("Downloading object '{}' from S3 bucket '{}'", objectName, bucketName);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest)) {
                byte[] data = response.readAllBytes();
                log.info("Successfully downloaded object '{}' from S3 bucket '{}', size: {} bytes", objectName, bucketName, data.length);
                return data;
            }
        } catch (NoSuchKeyException e) {
            log.warn("Object '{}' not found in S3 bucket '{}'", objectName, bucketName);
            return null;
        } catch (S3Exception e) {
            log.error("Failed to download object '{}' from S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to download object from S3: " + objectName, e);
        } catch (IOException e) {
            log.error("Failed to read object data '{}' from S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to read object data from S3: " + objectName, e);
        } catch (Exception e) {
            log.error("Unexpected error while downloading object '{}' from S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to download object from S3: " + objectName, e);
        }
    }

    @Override
    public Boolean delete(@Nonnull String bucketName, @Nonnull String objectName) {
        try {
            validateParameters(bucketName, objectName);

            log.info("Deleting object '{}' from S3 bucket '{}'", objectName, bucketName);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("Successfully deleted object '{}' from S3 bucket '{}'", objectName, bucketName);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete object '{}' from S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to delete object from S3: " + objectName, e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting object '{}' from S3 bucket '{}': {}", objectName, bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to delete object from S3: " + objectName, e);
        }
    }

    @Override
    public Collection<String> list(@Nonnull String bucketName) {
        try {
            validateBucketName(bucketName);

            log.info("Listing objects in S3 bucket '{}'", bucketName);

            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            List<String> objectNames = response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());

            log.info("Successfully listed {} objects in S3 bucket '{}'", objectNames.size(), bucketName);
            return objectNames;
        } catch (S3Exception e) {
            log.error("Failed to list objects in S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to list objects in S3 bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error while listing objects in S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new ObjectOperationException("Failed to list objects in S3 bucket: " + bucketName, e);
        }
    }

    private void validateParameters(String bucketName, String objectName) {
        validateBucketName(bucketName);
        if (StringUtils.isBlank(objectName)) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }
    }

    private void validateBucketName(String bucketName) {
        if (StringUtils.isBlank(bucketName)) {
            throw new IllegalArgumentException("Bucket name cannot be null or empty");
        }
    }
}
