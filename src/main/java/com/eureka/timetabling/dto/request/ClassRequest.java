package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/** Request DTO - Tạo lớp học */
@Data
public class ClassRequest {
    @NotNull
    private Long courseId;

    @NotBlank(message = "Tên lớp không được để trống")
    private String name;

    @Min(value = 1, message = "Số học viên phải lớn hơn 0")
    private Integer studentSize = 20;

    @NotNull(message = "Ngày khai giảng không được để trống")
    private LocalDate startDate;

    // Kỹ năng mặc định cho tất cả buổi học (có thể override)
    private String requiredSkill;
}
