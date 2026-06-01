package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Domain model cho cấu hình các ràng buộc của hệ thống xếp lịch giảng dạy.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConstraintConfig {
    private Long id;
    private String constraintKey;
    private String constraintName;
    private String description;
    private boolean enabled;
    private Integer weight;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
