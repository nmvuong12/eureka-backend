package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request DTO - Đăng nhập */
@Data
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
