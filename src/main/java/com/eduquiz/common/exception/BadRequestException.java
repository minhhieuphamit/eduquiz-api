package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;

public class BadRequestException extends BaseException {

    public BadRequestException(ResponseCode responseCode) {
        super(responseCode);
    }

    public BadRequestException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }

    public BadRequestException(String message) {
        super(ResponseCode.BAD_REQUEST, message);
    }
}
