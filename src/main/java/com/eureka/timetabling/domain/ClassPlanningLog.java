package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDateTime;

/** Audit log cho state machine lớp học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassPlanningLog {
    private Long id;
    private Long classId;
    /** Hành động: CREATED, STATUS_CHANGED, REBALANCED, MERGED, FORCE_OPENED, CANCELLED */
    private String action;
    private String oldStatus;
    private String newStatus;
    private String note;
    private LocalDateTime createdAt;
    private String createdBy;
}
