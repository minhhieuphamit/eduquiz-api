package com.eduquiz.feature.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Mã OTP không được để trống")
    private String otp;
}
