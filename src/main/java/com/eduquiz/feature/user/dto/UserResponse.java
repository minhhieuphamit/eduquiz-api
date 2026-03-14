package com.eduquiz.feature.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String phoneNumber;
    private String role;
    private Boolean isActive;
    private Boolean emailVerified;
    private LocalDateTime createdAt;
}
