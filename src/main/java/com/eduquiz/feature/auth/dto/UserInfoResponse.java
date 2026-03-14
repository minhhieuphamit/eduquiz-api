package com.eduquiz.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String phoneNumber;
    private String role;
}
