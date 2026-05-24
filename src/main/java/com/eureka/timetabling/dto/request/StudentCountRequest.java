package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request DTO - Cập nhật sĩ số học viên */
@Data
public class StudentCountRequest {
    @NotNull @Min(0) private Integer studentCount;
}
