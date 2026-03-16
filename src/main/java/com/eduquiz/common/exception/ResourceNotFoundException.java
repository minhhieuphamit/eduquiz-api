package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;

public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(ResponseCode responseCode) {
        super(responseCode);
    }

    public ResourceNotFoundException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }

    public ResourceNotFoundException(String message) {
        super(ResponseCode.RESOURCE_NOT_FOUND, message);
    }
}
