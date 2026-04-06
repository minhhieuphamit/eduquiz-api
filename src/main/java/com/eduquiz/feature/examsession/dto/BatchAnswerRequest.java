package com.eduquiz.feature.examsession.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchAnswerRequest {

    @NotEmpty(message = "answers list must not be empty")
    @Valid
    private List<AnswerRequest> answers;
}
