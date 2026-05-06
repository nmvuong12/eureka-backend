package com.eureka.timetabling.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** Request DTO - Tạo/cập nhật giáo viên */
@Data
public class TeacherRequest {
    @NotBlank(message = "Tên giáo viên không được để trống")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;
    private String status;
    private List<String> skills;
}
