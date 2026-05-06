package com.eureka.timetabling.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Domain entity - Giáo viên
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status; // ACTIVE, INACTIVE
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
