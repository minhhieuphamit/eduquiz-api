package com.eduquiz.feature.examsession.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class StartExamRequest {

    @NotNull(message = "examId is required")
    private UUID examId;

    // Optional: if student is joining through a room
    private UUID roomId;
}
