package com.eduquiz.feature.auth.dto;

import com.eduquiz.common.validation.StrongPassword;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Email không đúng định dạng"
    )
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @StrongPassword
    private String password;

    @NotBlank(message = "Họ không được để trống")
    @Size(max = 50, message = "Họ không được vượt quá 50 ký tự")
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 50, message = "Tên không được vượt quá 50 ký tự")
    private String lastName;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dob;

    @Pattern(regexp = "^(\\+?[0-9]{9,15})?$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Vai trò không được để trống")
    private String role;
}
