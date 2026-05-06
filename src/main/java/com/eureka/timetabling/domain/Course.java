package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain entity - Khóa học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    private Long id;
    private String name;
    private Integer totalLessons;
    private Integer defaultDuration;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
