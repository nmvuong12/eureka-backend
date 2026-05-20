package com.eureka.timetabling.service;

import com.eureka.timetabling.dto.request.TeacherRequest;
import com.eureka.timetabling.dto.request.TeacherAvailabilityRequest;
import com.eureka.timetabling.dto.response.TeacherResponse;
import com.eureka.timetabling.dto.response.PageResponse;

import java.util.List;

/**
 * Interface khai báo dịch vụ quản lý Giáo viên.
 * Định hình toàn bộ nghiệp vụ theo kiến trúc Clean Architecture.
 */
public interface TeacherService {

    /**
     * Lấy danh sách toàn bộ giáo viên theo trạng thái hoạt động (dữ liệu thô)
     *
     * @param status Trạng thái lọc (ACTIVE, INACTIVE, ON_LEAVE hoặc null)
     * @return Danh sách TeacherResponse đại diện giáo viên tìm thấy
     */
    List<TeacherResponse> findAll(String status);

    /**
     * Tìm kiếm giáo viên nâng cao với phân trang và sắp xếp động (Dynamic Specification style)
     *
     * @param teacherCode   Mã số giáo viên cần lọc (hoặc null)
     * @param fullName      Họ và tên giáo viên cần lọc (hoặc null)
     * @param skill         Kỹ năng giáo viên cần lọc (hoặc null)
     * @param teacherType   Loại hợp đồng giáo viên cần lọc (hoặc null)
     * @param workingStatus Trạng thái hoạt động cần lọc (hoặc null)
     * @param page          Số trang (1-indexed)
     * @param size          Kích thước trang
     * @param sortBy        Trường sắp xếp
     * @param sortDir       Chiều sắp xếp (ASC hoặc DESC)
     * @return Trang kết quả PageResponse
     */
    PageResponse<TeacherResponse> search(String teacherCode, String fullName, String skill,
                                         String teacherType, String workingStatus,
                                         int page, int size, String sortBy, String sortDir);

    /**
     * Lấy chi tiết thông tin giáo viên theo ID
     *
     * @param id ID của giáo viên
     * @return TeacherResponse đại diện giáo viên
     */
    TeacherResponse findById(Long id);

    /**
     * Thêm mới một giáo viên vào hệ thống
     *
     * @param request DTO chứa thông tin giáo viên cần thêm
     * @return TeacherResponse chứa thông tin giáo viên đã lưu (kèm mã giáo viên tự sinh)
     */
    TeacherResponse create(TeacherRequest request);

    /**
     * Cập nhật thông tin giáo viên hiện tại
     *
     * @param id      ID giáo viên cần cập nhật
     * @param request DTO chứa các thông tin thay đổi
     * @return TeacherResponse chứa thông tin giáo viên sau cập nhật
     */
    TeacherResponse update(Long id, TeacherRequest request);

    /**
     * Xóa giáo viên khỏi hệ thống (Xóa mềm - Soft Delete)
     * Ngăn chặn hành vi xóa nếu giáo viên đang có lịch giảng dạy đang hoạt động.
     *
     * @param id ID của giáo viên cần xóa
     */
    void delete(Long id);

    /**
     * Đăng ký lịch rảnh giảng dạy khả dụng cho giáo viên bán thời gian (PART_TIME)
     * Thực hiện kiểm tra ràng buộc loại giáo viên, giờ và xung đột trùng lịch (overlap).
     *
     * @param request DTO chứa thông tin đăng ký lịch rảnh
     */
    void registerAvailability(TeacherAvailabilityRequest request);

    /**
     * Lấy danh sách lịch rảnh đã đăng ký của giáo viên
     *
     * @param teacherId ID của giáo viên
     * @return Danh sách TeacherAvailability
     */
    List<com.eureka.timetabling.domain.TeacherAvailability> getAvailabilities(Long teacherId);

    /**
     * Xóa một lịch rảnh cụ thể theo ID
     *
     * @param id ID của lịch rảnh cần xóa
     */
    void deleteAvailability(Long id);
}
