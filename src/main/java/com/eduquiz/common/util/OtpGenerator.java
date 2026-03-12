package com.eduquiz.common.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Tạo mã OTP 6 số, zero-padded (vd: "001234").
     */
    public String generateOtp() {
        int otp = secureRandom.nextInt(1_000_000); // 0 → 999999
        return String.format("%06d", otp);
    }
}
