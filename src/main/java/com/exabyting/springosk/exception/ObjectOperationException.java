package com.exabyting.springosk.exception;

public class ObjectOperationException extends RuntimeException {
    public ObjectOperationException(String message) {
        super(message);
    }

    public ObjectOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ObjectOperationException(Throwable cause) {
        super(cause);
    }
}
