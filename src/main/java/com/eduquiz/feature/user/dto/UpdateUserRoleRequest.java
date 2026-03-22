package com.eduquiz.feature.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotBlank(message = "Vai trò không được để trống")
    private String role;
}
