package com.eduquiz.feature.examroom.dto;

import com.eduquiz.feature.examroom.entity.RoomStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RoomResponse {

    private UUID id;
    private String roomCode;
    private String title;

    private UUID examId;
    private String examTitle;
    private String subjectName;
    private String teacherName;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RoomStatus status;

    private Integer maxStudents;

    /** Effective duration in minutes (room override if set, else exam's duration). */
    private Integer durationMinutes;

    private int participantCount;
    private int submittedCount;
}
