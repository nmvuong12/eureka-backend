package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO - Đăng ký lịch rảnh của giáo viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityRequest {

    /**
     * ID của giáo viên đăng ký lịch rảnh
     */
    @NotNull(message = "ID giáo viên không được để trống")
    private Long teacherId;

    /**
     * Thứ trong tuần (VD: MONDAY, TUESDAY, ..., SUNDAY)
     */
    @NotBlank(message = "Thứ trong tuần không được để trống")
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$", 
            message = "Thứ phải là: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY")
    private String dayOfWeek;

    /**
     * Giờ bắt đầu rảnh (VD: "18:00")
     */
    @NotBlank(message = "Giờ bắt đầu không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ bắt đầu phải đúng định dạng HH:mm")
    private String startTime;

    /**
     * Giờ kết thúc rảnh (VD: "20:00")
     */
    @NotBlank(message = "Giờ kết thúc không được để trống")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Giờ kết thúc phải đúng định dạng HH:mm")
    private String endTime;
}
