package com.eureka.timetabling.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Domain Entity - Đăng ký lịch rảnh của giáo viên
 * Áp dụng đối với giáo viên bán thời gian (PART_TIME) để đăng ký thời gian giảng dạy khả dụng.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAvailability {

    /**
     * ID tự tăng của lịch rảnh
     */
    private Long id;

    /**
     * ID của giáo viên đăng ký lịch rảnh
     */
    private Long teacherId;

    /**
     * Thứ trong tuần (VD: MONDAY, TUESDAY, ...)
     */
    private String dayOfWeek;

    /**
     * Giờ bắt đầu rảnh (VD: 18:00:00)
     */
    private LocalTime startTime;

    /**
     * Giờ kết thúc rảnh (VD: 20:00:00)
     */
    private LocalTime endTime;
}
