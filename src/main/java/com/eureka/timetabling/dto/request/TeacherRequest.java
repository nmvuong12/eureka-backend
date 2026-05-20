package com.eureka.timetabling.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO - Tạo mới hoặc Cập nhật giáo viên
 * Chứa các ràng buộc kiểm tra tính hợp lệ của dữ liệu đầu vào.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequest {

    /**
     * Phân loại hợp đồng: FULL_TIME hoặc PART_TIME
     */
    @NotBlank(message = "Loại giáo viên không được để trống (FULL_TIME hoặc PART_TIME)")
    @Pattern(regexp = "^(FULL_TIME|PART_TIME)$", message = "Loại giáo viên phải là FULL_TIME hoặc PART_TIME")
    private String teacherType;

    /**
     * Họ và tên đầy đủ của giáo viên
     */
    @JsonAlias({"name", "fullName"})
    @NotBlank(message = "Họ và tên giáo viên không được để trống")
    @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
    private String fullName;

    /**
     * Ngày sinh của giáo viên (phải nhỏ hơn hoặc bằng ngày hiện tại)
     */
    @PastOrPresent(message = "Ngày sinh không được lớn hơn ngày hiện tại")
    private LocalDate dateOfBirth;

    /**
     * Giới tính: MALE, FEMALE hoặc OTHER
     */
    @NotBlank(message = "Giới tính không được để trống (MALE, FEMALE hoặc OTHER)")
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
    private String gender;

    /**
     * Địa chỉ cư trú
     */
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    /**
     * Địa chỉ Email (Phải đúng định dạng)
     */
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 200, message = "Email không được vượt quá 200 ký tự")
    private String email;

    /**
     * Số điện thoại (Phải đúng định dạng Việt Nam)
     */
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[35789][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String phone;

    /**
     * Chuỗi mô tả kỹ năng
     */
    @Size(max = 500, message = "Kỹ năng không được vượt quá 500 ký tự")
    private String skills;

    /**
     * Tên file chứng chỉ đính kèm
     */
    @Size(max = 255, message = "Tên file chứng chỉ không được vượt quá 255 ký tự")
    private String certificateFile;

    /**
     * Tên file hồ sơ cá nhân đính kèm
     */
    @Size(max = 255, message = "Tên file hồ sơ không được vượt quá 255 ký tự")
    private String profileFile;

    /**
     * Trạng thái hoạt động: ACTIVE, INACTIVE, ON_LEAVE
     */
    @JsonAlias({"status", "workingStatus"})
    @NotBlank(message = "Trạng thái hoạt động không được để trống")
    @Pattern(regexp = "^(ACTIVE|INACTIVE|ON_LEAVE)$", message = "Trạng thái phải là ACTIVE, INACTIVE hoặc ON_LEAVE")
    private String workingStatus;
}
