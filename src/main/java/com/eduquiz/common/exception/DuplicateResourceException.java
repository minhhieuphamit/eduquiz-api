package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(ResponseCode responseCode) {
        super(responseCode);
    }

    public DuplicateResourceException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }

    public DuplicateResourceException(String message) {
        super(ResponseCode.DUPLICATE_RESOURCE, message);
    }
}
