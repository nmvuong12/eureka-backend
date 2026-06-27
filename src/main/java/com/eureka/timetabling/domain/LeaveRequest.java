package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain entity - Đơn xin nghỉ */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveRequest {
    private Long id;
    private Long teacherId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED
    private String sessionType; // MORNING, AFTERNOON, ALL_DAY
    private String makeupOption; // MAKEUP, NO_MAKEUP
    private LocalDate makeupDate;
    private Long makeupTimeslotId;
    private String dayConfigs; // JSON list of per-day configs (date, sessionType)
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient fields
    private String teacherName;
    private String teacherCode;
}

