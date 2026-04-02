package com.eduquiz.feature.examroom.dto;

import com.eduquiz.feature.examroom.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer durationMinutes;
    private long participantCount;
    private long submittedCount;
}
