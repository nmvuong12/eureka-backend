package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    
    private String password;
    
    @NotBlank(message = "Role không được để trống")
    private String role;
    
    private Long teacherId;
    private boolean active = true;
}
