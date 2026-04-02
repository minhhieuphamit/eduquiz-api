package com.eduquiz.feature.examsession.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartExamRequest {

    @NotNull(message = "Đề thi không được để trống")
    private UUID examId;

    /** Nullable — null cho luyện tập tự do */
    private UUID roomId;
}
