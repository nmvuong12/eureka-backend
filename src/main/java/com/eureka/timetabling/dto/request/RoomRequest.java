package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request DTO - Tạo phòng học */
@Data
public class RoomRequest {
    @NotBlank(message = "Tên phòng không được để trống")
    private String name;

    @NotNull
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;

    private String status;
}
