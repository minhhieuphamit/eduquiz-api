package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;
import com.eduquiz.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException ex) {
        ResponseCode rc = ex.getResponseCode();
        return ResponseEntity.status(resolveHttpStatus(rc))
                .body(ApiResponse.error(rc, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ResponseCode.AUTH_FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ResponseCode.AUTH_UNAUTHORIZED));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ResponseCode.VALIDATION_ERROR, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private HttpStatus resolveHttpStatus(ResponseCode rc) {
        int code = rc.getCode();
        if (code >= 2401 && code <= 2406) return HttpStatus.UNAUTHORIZED;
        if (code >= 2407 && code <= 2409) return HttpStatus.FORBIDDEN;

        return switch (code % 1000 / 100) {
            case 4 -> {
                int suffix = code % 100;
                if (suffix == 1) yield HttpStatus.UNAUTHORIZED;
                if (suffix == 4) yield HttpStatus.NOT_FOUND;
                if (suffix == 9) yield HttpStatus.CONFLICT;
                if (suffix == 29) yield HttpStatus.TOO_MANY_REQUESTS;
                yield HttpStatus.BAD_REQUEST;
            }
            case 5 -> {
                if (code >= 2500 && code <= 2599) {
                    if (code == 2504 || code == 2505) yield HttpStatus.TOO_MANY_REQUESTS;
                    if (code == 2506) yield HttpStatus.NOT_FOUND;
                    if (code == 2507) yield HttpStatus.CONFLICT;
                    if (code == 2508) yield HttpStatus.INTERNAL_SERVER_ERROR;
                    yield HttpStatus.BAD_REQUEST;
                }
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
            default -> HttpStatus.OK;
        };
    }
}
