package com.eduquiz.common.exception;

/**
 * Custom exception.
 * TODO: Implement extends RuntimeException
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
