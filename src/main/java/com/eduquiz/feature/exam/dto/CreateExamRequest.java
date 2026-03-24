package com.eduquiz.feature.exam.dto;

import com.eduquiz.feature.exam.entity.ExamType;
import com.eduquiz.feature.exam.entity.RandomMode;
import com.eduquiz.feature.question.entity.Difficulty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExamRequest {

    @NotBlank(message = "Tiêu đề đề thi không được để trống")
    @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
    private String title;

    private String description;

    @NotNull(message = "Môn học không được để trống")
    private UUID subjectId;

    @Min(value = 1, message = "Thời gian làm bài phải lớn hơn 0")
    private Integer durationMinutes;

    @NotNull(message = "Chế độ tạo đề không được để trống")
    private RandomMode randomMode;

    private Integer year;

    private ExamType examType;

    @Min(value = 1, message = "Số câu hỏi phải lớn hơn 0")
    private Integer totalQuestions;

    // Cho MANUAL mode: danh sách câu hỏi được chọn
    private List<UUID> questionIds;

    // Cho POOL_RANDOM mode: chọn chapters + difficulty
    private List<UUID> chapterIds;
    private Difficulty difficulty;
}
