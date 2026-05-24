package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/** Request DTO - Tạo/cập nhật kế hoạch khai giảng */
@Data
public class CourseBatchRequest {
    @NotNull(message = "Vui lòng chọn khóa học")
    private Long courseId;

    @NotBlank(message = "Tên kế hoạch không được bỏ trống")
    private String batchName;

    private LocalDate enrollmentStartDate;
    private LocalDate enrollmentEndDate;
    private LocalDate expectedOpeningDate;

    @Min(0) private Integer forecastScale;
    private String note;
}
