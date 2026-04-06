package com.eduquiz.feature.examsession.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AnswerRequest {

    @NotNull(message = "questionId is required")
    private UUID questionId;

    /**
     * For SINGLE_CHOICE: list with one element, e.g. ["A"]
     * For MULTI_CHOICE:  list with multiple elements, e.g. ["A", "C"]
     * Empty list or null = clear/unanswer the question
     */
    private List<String> selectedOptions;
}
