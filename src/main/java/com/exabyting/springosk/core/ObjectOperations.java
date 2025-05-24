package com.exabyting.springosk.core;

import java.util.Collection;

/**
 * Interface for object operations such as upload, download, delete, and list objects in a bucket.
 */
public interface ObjectOperations {
    /**
     * Uploads an object to the specified bucket.
     *
     * @param bucketName the name of the bucket
     * @param objectName the name of the object to upload
     * @param data the data to upload
     * @return true if the upload was successful, false otherwise
     */
    Boolean upload(String bucketName, String objectName, byte[] data);

    /**
     * Downloads an object from the specified bucket.
     *
     * @param bucketName the name of the bucket
     * @param objectName the name of the object to download
     * @return the data of the object, or null if not found
     */
    byte[] download(String bucketName, String objectName);

    /**
     * Deletes an object from the specified bucket.
     *
     * @param bucketName the name of the bucket
     * @param objectName the name of the object to delete
     * @return true if the object was deleted successfully, false otherwise
     */
    Boolean delete(String bucketName, String objectName);

    /**
     * Lists all object names in the specified bucket.
     *
     * @param bucketName the name of the bucket
     * @return a collection of object names in the bucket
     */
    Collection<String> list(String bucketName);
}
