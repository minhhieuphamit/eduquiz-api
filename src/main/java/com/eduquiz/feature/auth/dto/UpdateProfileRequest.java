package com.eduquiz.feature.auth.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private String phoneNumber;
}
