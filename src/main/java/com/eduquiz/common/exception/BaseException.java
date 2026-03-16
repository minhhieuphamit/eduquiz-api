package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ResponseCode responseCode;

    protected BaseException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    protected BaseException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }
}
