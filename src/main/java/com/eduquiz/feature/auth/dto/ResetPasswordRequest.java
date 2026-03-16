package com.eduquiz.feature.auth.dto;

import com.eduquiz.common.validation.StrongPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải có 6 ký tự")
    private String otp;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @StrongPassword
    private String newPassword;
}
