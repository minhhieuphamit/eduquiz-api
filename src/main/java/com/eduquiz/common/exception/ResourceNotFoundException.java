package com.eduquiz.common.exception;

/**
 * Custom exception.
 * TODO: Implement extends RuntimeException
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
