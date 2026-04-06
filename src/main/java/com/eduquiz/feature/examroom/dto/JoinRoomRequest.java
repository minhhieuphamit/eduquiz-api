package com.eduquiz.feature.examroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinRoomRequest {

    @NotBlank
    @Size(min = 4, max = 10)
    private String roomCode;
}
