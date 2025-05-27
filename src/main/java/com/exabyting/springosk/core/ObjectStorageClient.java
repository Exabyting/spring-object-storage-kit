package com.exabyting.springosk.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ObjectStorageClient {
    private final BucketOperations bucketOperations;
    private final ObjectOperations objectOperations;

    // Bucket Operations
    public boolean createBucket(String bucketName) {
        return bucketOperations.create(bucketName);
    }

    public boolean deleteBucket(String bucketName) {
        return bucketOperations.delete(bucketName);
    }

    public List<String> listBuckets() {
        return List.copyOf(bucketOperations.getAllBuckets());
    }

    // Object Operations
    public Boolean putObject(String bucketName, String objectKey, byte[] inputStream) {
        return objectOperations.upload(bucketName, objectKey, inputStream);
    }

    public boolean deleteObject(String bucketName, String objectKey) {
        return objectOperations.delete(bucketName, objectKey);
    }

    public List<String> listObjects(String bucketName) {
        return List.copyOf(objectOperations.list(bucketName));
    }
}
