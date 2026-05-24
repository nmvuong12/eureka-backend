package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain entity - Lớp học thực tế */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolClass {
    private Long id;
    /** Mã lớp duy nhất (VD: IELTS_A) */
    private String classCode;
    /** FK tới course_batch */
    private Long batchId;
    /** FK tới schedule_pattern */
    private Long schedulePatternId;
    /** Giáo viên phụ trách (nullable, gán sau khi solver chạy) */
    private Long teacherId;
    /** Phòng học (nullable, gán sau khi solver chạy) */
    private Long roomId;
    /** Sĩ số thực tế */
    private Integer studentCount;
    /** Trạng thái lớp */
    private ClassStatus status;
    /** Ngày khai giảng thực tế */
    private LocalDate actualOpeningDate;
    // Legacy fields - giữ tương thích ngược
    private Long courseId;
    private String name;         // = classCode
    private Integer studentSize; // = studentCount
    private LocalDate startDate; // = actualOpeningDate
    private String note;         // Ghi chú lớp học
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Join fields (không lưu trong bảng)
    private String batchName;
    private String courseName;
    private String courseCode;
    private String patternCode;
    private String patternLabel;
    private String teacherName;
}
