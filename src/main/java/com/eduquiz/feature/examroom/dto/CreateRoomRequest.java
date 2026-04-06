package com.eduquiz.feature.examroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateRoomRequest {

    @NotBlank
    private String title;

    @NotNull
    private UUID examId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private Integer maxStudents;

    /** Only relevant for PRACTICE exams — overrides the exam's default durationMinutes. */
    private Integer durationMinutes;
}
