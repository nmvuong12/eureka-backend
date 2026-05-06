package com.eureka.timetabling.dto.response;

import lombok.*;

/** Response DTO - Đăng nhập */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String role;
    private Long teacherId;
    private String tokenType = "Bearer";
}
