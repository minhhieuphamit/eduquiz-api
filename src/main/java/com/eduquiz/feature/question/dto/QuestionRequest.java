package com.eduquiz.feature.question.dto;

import com.eduquiz.feature.question.entity.Difficulty;
import com.eduquiz.feature.question.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String content;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private QuestionType type;

    @NotNull(message = "Độ khó không được để trống")
    private Difficulty difficulty;

    private String explanation;

    @Valid
    @NotNull(message = "Danh sách đáp án không được để trống")
    @Size(min = 2, max = 6, message = "Câu hỏi phải có từ 2 đến 6 đáp án")
    private List<OptionRequest> options;
}
