package com.eduquiz.feature.auth.dto;

import com.eduquiz.feature.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long id;
    private String email;
    private String fullName;
    private Role role;
}
