package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Domain entity - Lời mời dạy thay FCFS */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SubstituteOffer {
    private Long id;
    private Long leaveRequestId;
    private Long lessonId;
    private Long teacherId;
    private String token;
    private String status; // PENDING, ACCEPTED, EXPIRED
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    // Join fields for UI display
    private String classCode;
    private Integer lessonIndex;
    private LocalDate sessionDate;
    private Long timeslotId;
    private String timeslotLabel;
    private String originalTeacherName;
    private String requiredSkill;
}
