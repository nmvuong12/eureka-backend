package com.eureka.timetabling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Response DTO - Thông tin giáo viên đầy đủ */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TeacherResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private List<String> skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
