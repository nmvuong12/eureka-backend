package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Kế hoạch khai giảng - trung gian giữa Course và Class */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseBatch {
    private Long id;
    /** FK tới course */
    private Long courseId;
    /** Tên hiển thị (VD: IELTS tháng 7/2026) */
    private String batchName;
    /** Ngày bắt đầu tuyển sinh */
    private LocalDate enrollmentStartDate;
    /** Ngày kết thúc tuyển sinh */
    private LocalDate enrollmentEndDate;
    /** Ngày khai giảng dự kiến */
    private LocalDate expectedOpeningDate;
    /** Quy mô dự kiến (số học viên muốn tuyển) */
    private Integer forecastScale;
    /** Trạng thái kế hoạch */
    private CourseBatchStatus status;
    /** Ghi chú kế hoạch */
    private String note;
    /** Số lớp đã sinh từ kế hoạch này (Dynamic field) */
    private Integer generatedClassCount;
    // Thông tin join từ course (không lưu trong bảng)
    private String courseName;
    private String courseCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
