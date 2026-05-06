package com.eureka.timetabling.domain;

import lombok.*;

import java.time.LocalDateTime;

/** Domain entity - Phòng học */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    private Long id;
    private String name;
    private Integer capacity;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
