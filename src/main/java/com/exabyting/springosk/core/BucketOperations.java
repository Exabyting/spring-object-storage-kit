package com.exabyting.springosk.core;

import java.util.Collection;

/**
 * Interface for bucket operations such as create, delete, and list buckets.
 */
public interface BucketOperations {
    /**
     * Creates a new bucket with the specified name.
     *
     * @param bucketName the name of the bucket to create
     * @return true if the bucket was created successfully, false otherwise
     */
    Boolean create(String bucketName);

    /**
     * Deletes the bucket with the specified name.
     *
     * @param bucketName the name of the bucket to delete
     * @return true if the bucket was deleted successfully, false otherwise
     */
    Boolean delete(String bucketName);

    /**
     * Retrieves the names of all existing buckets.
     *
     * @return a collection of bucket names
     */
    Collection<String> getAllBuckets();
}
