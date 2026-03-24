package com.eduquiz.feature.chapter.dto;

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
public class ChapterResponse {
    private UUID id;
    private UUID subjectId;
    private String subjectName;
    private String name;
    private String description;
    private Integer orderIndex;
    private Long questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
