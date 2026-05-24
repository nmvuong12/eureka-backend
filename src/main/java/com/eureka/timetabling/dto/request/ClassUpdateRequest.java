package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO - Cập nhật thông tin chi tiết lớp học
 */
@Data
public class ClassUpdateRequest {
    @NotBlank(message = "Tên lớp học không được bỏ trống")
    private String name;

    @Min(value = 0, message = "Sĩ số tối đa không được nhỏ hơn 0")
    private Integer studentSize;

    private Long teacherId;
    
    private String note;
}
