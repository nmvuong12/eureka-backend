package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO chứa các quy tắc xác thực cho Mẫu lịch học
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulePatternRequest {

    @NotBlank(message = "Ngày học không được để trống")
    private String studyDays; // e.g. "2,4,6"

    @NotBlank(message = "Mã ca học không được để trống")
    private String slotCode; // e.g. "C1"

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    private String slotStart; // e.g. "08:00"

    @NotBlank(message = "Giờ kết thúc không được để trống")
    private String slotEnd; // e.g. "10:00"

    @NotNull(message = "Số buổi học trên tuần không được để trống")
    @Min(value = 1, message = "Số buổi học trên tuần tối thiểu là 1")
    @Max(value = 7, message = "Số buổi học trên tuần tối đa là 7")
    private Integer sessionsPerWeek;

    private Boolean active;
}
