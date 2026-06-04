package com.eureka.timetabling.domain;

import lombok.*;
import java.time.LocalDateTime;

/** Domain entity - Thông báo đẩy trên Web */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WebNotification {
    private Long id;
    private String recipientRole; // ADMIN, STAFF
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
