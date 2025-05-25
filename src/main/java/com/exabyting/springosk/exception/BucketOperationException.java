package com.exabyting.springosk.exception;

public class BucketOperationException extends RuntimeException {
    public BucketOperationException(String message) {
        super(message);
    }

    public BucketOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BucketOperationException(Throwable cause) {
        super(cause);
    }
}
