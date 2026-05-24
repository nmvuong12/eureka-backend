package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/** Request DTO - Tạo/cập nhật khóa học */
@Data
public class CourseRequest {
    @NotBlank(message = "Mã khóa học không được bỏ trống")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã khóa học chỉ chứa chữ hoa, số và dấu gạch dưới")
    private String code;

    @NotBlank(message = "Tên khóa học không được bỏ trống")
    private String name;

    private String description;

    @NotNull @Min(1) private Integer totalSessions;
    @NotNull @Min(1) @Max(7) private Integer sessionsPerWeek;
    @NotNull @Min(1) private Integer durationWeeks;
    @NotNull @Min(1) private Integer minStudents;
    @NotNull @Min(1) private Integer maxStudents;

    private BigDecimal tuitionFee;
    private String requiredSkillCode;
    /** Thời lượng mỗi buổi học (phút), mặc định 120 phút */
    private Integer defaultDuration;
}
