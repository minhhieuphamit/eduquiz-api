package com.eduquiz.feature.subject.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubjectRequest {

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 100, message = "Tên môn học không được vượt quá 100 ký tự")
    private String name;

    private String description;

    @Min(value = 1, message = "Thời lượng mặc định phải lớn hơn 0")
    private Integer defaultDurationMinutes;
}
