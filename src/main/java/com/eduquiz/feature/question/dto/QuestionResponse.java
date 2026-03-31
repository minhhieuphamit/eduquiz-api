package com.eduquiz.feature.question.dto;

import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private UUID id;
    private UUID chapterId;
    private String chapterName;
    private UUID subjectId;
    private String subjectName;
    private String content;
    private QuestionType type;
    private Difficulty difficulty;
    private String explanation;
    private List<OptionResponse> options;

    // Audit fields
    private UUID createdById;
    private String createdByName;
    private UUID updatedById;
    private String updatedByName;

    // Context fields (populated based on current user)
    private Boolean isOwner;
    private Boolean isShared;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
