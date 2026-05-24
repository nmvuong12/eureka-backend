package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Request DTO - Tạo lớp học nháp hàng loạt */
@Data
public class GenerateClassRequest {
    @NotNull private Long batchId;
    @NotNull private Long patternId;
    /** Số lớp cần tạo (1-20) */
    @NotNull @Min(1) @Max(20) private Integer count;
}
