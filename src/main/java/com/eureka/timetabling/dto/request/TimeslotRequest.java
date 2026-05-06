package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Request DTO - Tạo ca học */
@Data
public class TimeslotRequest {
    @NotBlank(message = "Ngày trong tuần không được để trống")
    private String dayOfWeek;

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    private String startTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    private String endTime;

    private String label;
}
