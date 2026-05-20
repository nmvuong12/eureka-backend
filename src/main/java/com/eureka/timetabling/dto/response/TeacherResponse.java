package com.eureka.timetabling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO - Thông tin chi tiết đầy đủ của giáo viên
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {

    /**
     * ID tự tăng của giáo viên
     */
    private Long id;

    /**
     * Mã số giáo viên (VD: GV0001)
     */
    private String teacherCode;

    /**
     * Loại giáo viên (FULL_TIME hoặc PART_TIME)
     */
    private String teacherType;

    /**
     * Họ và tên đầy đủ
     */
    private String fullName;

    /**
     * Ngày sinh của giáo viên
     */
    private LocalDate dateOfBirth;

    /**
     * Giới tính (MALE, FEMALE, OTHER)
     */
    private String gender;

    /**
     * Địa chỉ cư trú
     */
    private String address;

    /**
     * Địa chỉ Email
     */
    private String email;

    /**
     * Số điện thoại liên hệ
     */
    private String phone;

    /**
     * Các kỹ năng (VD: "IELTS 8.0, Speaking")
     */
    private String skills;

    /**
     * Đường dẫn hoặc tên tệp tin chứng chỉ đính kèm
     */
    private String certificateFile;

    /**
     * Đường dẫn hoặc tên tệp tin hồ sơ cá nhân đính kèm
     */
    private String profileFile;

    /**
     * Trạng thái hoạt động (ACTIVE, INACTIVE, ON_LEAVE)
     */
    private String workingStatus;

    /**
     * Thời điểm tạo hồ sơ
     */
    private LocalDateTime createdDate;

    /**
     * Thời điểm cập nhật hồ sơ gần nhất
     */
    private LocalDateTime modifiedDate;

    /**
     * Getter tương thích ngược cho thuộc tính 'name'
     */
    public String getName() {
        return fullName;
    }

    /**
     * Getter tương thích ngược cho thuộc tính 'status'
     */
    public String getStatus() {
        return workingStatus;
    }
}
