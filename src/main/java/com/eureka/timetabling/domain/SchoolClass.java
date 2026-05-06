package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain entity - Lớp học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SchoolClass {
    private Long id;
    private Long courseId;
    private String name;
    private Integer studentSize;
    private LocalDate startDate;
    private String status; // PENDING, ACTIVE, COMPLETED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
