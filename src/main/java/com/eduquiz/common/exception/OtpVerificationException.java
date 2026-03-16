package com.eduquiz.common.exception;

import com.eduquiz.common.constant.ResponseCode;

public class OtpVerificationException extends BaseException {

    public OtpVerificationException(ResponseCode responseCode) {
        super(responseCode);
    }

    public OtpVerificationException(ResponseCode responseCode, String message) {
        super(responseCode, message);
    }

    public OtpVerificationException(String message) {
        super(ResponseCode.OTP_INVALID, message);
    }
}
