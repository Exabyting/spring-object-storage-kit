package com.exabyting.springosk.s3;

import com.exabyting.springosk.exception.BucketOperationException;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;
import com.exabyting.springosk.annotation.ConditionalOnStorageType;
import com.exabyting.springosk.core.BucketOperations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnStorageType
public class S3BucketOperations implements BucketOperations {

    private final S3Client s3Client;

    @Override
    public Boolean create(@Nonnull String bucketName) {
        try {
            if (bucketName.isBlank()) {
                log.warn("Bucket name cannot be null or empty");
                return false;
            }
            log.info("Creating S3 bucket: {}", bucketName);
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.createBucket(createBucketRequest);
            log.info("Successfully created S3 bucket: {}", bucketName);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to create S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to create S3 bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error while creating S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to create S3 bucket: " + bucketName, e);
        }
    }

    @Override
    public Boolean delete(@Nonnull String bucketName) {
        try {
            if (bucketName == null || bucketName.isBlank()) {
                log.warn("Bucket name cannot be null or empty");
                return false;
            }
            log.info("Deleting S3 bucket: {}", bucketName);
            // First, delete all objects in the bucket
            deleteAllObjectsInBucket(bucketName);

            // Then delete the bucket itself
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.deleteBucket(deleteBucketRequest);
            log.info("Successfully deleted S3 bucket: {}", bucketName);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to delete S3 bucket: " + bucketName, e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting S3 bucket '{}': {}", bucketName, e.getMessage(), e);
            throw new BucketOperationException("Failed to delete S3 bucket: " + bucketName, e);
        }
    }

    @Override
    public Collection<String> getAllBuckets() {
        try {
            log.debug("Listing all S3 buckets");
            ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);

            List<String> bucketNames = listBucketsResponse.buckets().stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList());

            log.debug("Found {} S3 buckets", bucketNames.size());
            return bucketNames;
        } catch (S3Exception e) {
            log.error("Failed to list S3 buckets: {}", e.getMessage(), e);
            throw new BucketOperationException("Failed to list S3 buckets", e);
        } catch (Exception e) {
            log.error("Unexpected error while listing S3 buckets: {}", e.getMessage(), e);
            throw new BucketOperationException("Failed to list S3 buckets", e);
        }
    }

    private void deleteAllObjectsInBucket(@Nonnull String bucketName) {
        try {
            if (bucketName == null || bucketName.isBlank()) {
                log.warn("Bucket name cannot be null or empty");
                return;
            }
            log.debug("Deleting all objects in bucket: {}", bucketName);

            ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
                    .bucket(bucketName)
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
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();
                s3Client.deleteObjects(deleteObjectsRequest);
                log.debug("Successfully deleted {} objects from bucket: {}", objectsToDelete.size(), bucketName);
            } else {
                log.debug("No objects found in bucket {} to delete", bucketName);
            }
        } catch (S3Exception e) {
            log.warn("Failed to delete objects in bucket '{}': {}", bucketName, e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while deleting objects in bucket '{}': {}", bucketName, e.getMessage());
        }
    }
}
