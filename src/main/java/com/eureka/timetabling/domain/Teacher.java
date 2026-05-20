package com.eureka.timetabling.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain Entity - Giáo viên
 * Chứa thông tin chi tiết và đầy đủ về hồ sơ giáo viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    /**
     * ID tự tăng của giáo viên trong cơ sở dữ liệu
     */
    private Long id;

    /**
     * Mã số giáo viên duy nhất (VD: GV0001, GV0002)
     */
    private String teacherCode;

    /**
     * Phân loại giáo viên: FULL_TIME (Toàn thời gian) hoặc PART_TIME (Bán thời gian)
     */
    private TeacherType teacherType;

    /**
     * Họ và tên đầy đủ của giáo viên
     */
    private String fullName;

    /**
     * Ngày sinh của giáo viên
     */
    private LocalDate dateOfBirth;

    /**
     * Giới tính: MALE, FEMALE hoặc OTHER
     */
    private Gender gender;

    /**
     * Địa chỉ cư trú hiện tại
     */
    private String address;

    /**
     * Địa chỉ Email liên hệ (Duy nhất)
     */
    private String email;

    /**
     * Số điện thoại liên hệ (Duy nhất)
     */
    private String phone;

    /**
     * Các kỹ năng bổ sung (VD: "IELTS 8.0, Speaking")
     */
    private String skills;

    /**
     * Tên tệp tin chứng chỉ chuyên môn đính kèm
     */
    private String certificateFile;

    /**
     * Tên tệp tin hồ sơ cá nhân đính kèm
     */
    private String profileFile;

    /**
     * Trạng thái hoạt động: ACTIVE (Hoạt động), INACTIVE (Ngừng hoạt động), ON_LEAVE (Nghỉ phép)
     */
    private WorkingStatus workingStatus;

    /**
     * Cờ đánh dấu xóa mềm: true (Đã xóa), false (Chưa xóa)
     */
    private boolean deleted;

    /**
     * Thời gian tạo bản ghi
     */
    private LocalDateTime createdDate;

    /**
     * Thời gian cập nhật bản ghi gần nhất
     */
    private LocalDateTime modifiedDate;
}
