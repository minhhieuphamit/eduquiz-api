package com.eduquiz.feature.auth.dto;

import com.eduquiz.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordInitRequest {
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @StrongPassword
    private String newPassword;
}
