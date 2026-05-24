package com.eureka.timetabling.dto.response;

import lombok.*;

/** DTO - Thông tin capacity của một schedule pattern */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CapacityDashboardItem {
    private Long patternId;
    private String patternCode;
    private String studyDays;
    private String slotCode;
    private String slotStart;
    private String slotEnd;
    private int sessionsPerWeek;
    /** Nhãn hiển thị (VD: T2-T4-T6 C5 19:00-21:00) */
    private String displayLabel;
    /** Công suất tối đa = MIN(phòng rảnh, GV rảnh có kỹ năng) */
    private int capacity;
    /** Số lớp đang dùng pattern này (OPEN + STUDYING) */
    private int occupied;
    /** Số slot còn lại = capacity - occupied */
    private int remaining;
    /** Số phòng ACTIVE không bận vào slot này */
    private int availableRooms;
    /** Số GV ACTIVE có kỹ năng phù hợp không bận vào slot này */
    private int availableTeachers;
}
