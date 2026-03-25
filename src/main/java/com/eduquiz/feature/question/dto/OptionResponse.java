package com.eduquiz.feature.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {
    private UUID id;
    private String label;
    private String content;
    private Boolean isCorrect;
    private Integer orderIndex;
}
