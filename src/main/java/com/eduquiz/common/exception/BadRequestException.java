package com.eduquiz.common.exception;

/**
 * Custom exception.
 * TODO: Implement extends RuntimeException
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
