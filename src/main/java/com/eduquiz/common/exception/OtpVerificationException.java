package com.eduquiz.common.exception;

/**
 * Custom exception.
 * TODO: Implement extends RuntimeException
 */
public class OtpVerificationException extends RuntimeException {
    public OtpVerificationException(String message) {
        super(message);
    }
}
