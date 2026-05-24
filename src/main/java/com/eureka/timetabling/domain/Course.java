package com.eureka.timetabling.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Domain entity - Khóa học đào tạo chuẩn (Course Catalog) */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    private Long id;
    /** Mã khóa học duy nhất (VD: IELTS_65) */
    private String code;
    /** Tên khóa học đầy đủ */
    private String name;
    /** Mô tả chương trình */
    private String description;
    /** Tổng số buổi học */
    private Integer totalSessions;
    /** Số buổi học mỗi tuần */
    private Integer sessionsPerWeek;
    /** Tổng số tuần học */
    private Integer durationWeeks;
    /** Sĩ số tối thiểu để mở lớp */
    private Integer minStudents;
    /** Sĩ số tối đa mỗi lớp */
    private Integer maxStudents;
    /** Học phí */
    private BigDecimal tuitionFee;
    /** Mã kỹ năng giáo viên cần có (VD: IELTS) */
    private String requiredSkillCode;
    /** Trạng thái áp dụng */
    private CourseStatus status;
    /** Legacy fields - giữ tương thích ngược */
    private Integer totalLessons;      // = totalSessions
    private Integer defaultDuration;   // phút/buổi, mặc định 120
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
