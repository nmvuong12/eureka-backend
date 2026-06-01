package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.request.TeacherRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.dto.response.TimetableEntryResponse;
import com.eureka.timetabling.dto.response.TeacherResponse;
import com.eureka.timetabling.service.TeacherService;
import com.eureka.timetabling.service.impl.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các API quản lý thông tin Giáo viên và Lịch rảnh.
 * Hỗ trợ Swagger UI, kiểm tra tính hợp lệ dữ liệu và bảo mật phân quyền đầu cuối.
 */
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Tag(name = "Giáo viên", description = "API Quản lý thông tin và lịch biểu của giáo viên")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService teacherService;
    private final TimetableService timetableService;

    /**
     * API Tìm kiếm giáo viên nâng cao (Hỗ trợ phân trang, sắp xếp và query động)
     * Quyền hạn: ADMIN, STAFF, TEACHER
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Tìm kiếm & Phân trang giáo viên", 
               description = "Lấy danh sách giáo viên dựa trên các bộ lọc tùy chọn với hỗ trợ phân trang và sắp xếp động.")
    public ResponseEntity<ApiResponse<PageResponse<TeacherResponse>>> search(
            @RequestParam(required = false) String teacherCode,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String teacherType,
            @RequestParam(required = false) String workingStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "teacher_code") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        
        PageResponse<TeacherResponse> response = teacherService.search(
                teacherCode, fullName, skill, teacherType, workingStatus, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * API Lấy toàn bộ danh sách giáo viên không phân trang
     * Quyền hạn: ADMIN, STAFF, TEACHER
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Lấy toàn bộ danh sách giáo viên không phân trang", 
               description = "Trả về mảng danh sách toàn bộ giáo viên chưa xóa để hiển thị lên các ô chọn (Select Dropdowns).")
    public ResponseEntity<ApiResponse<List<TeacherResponse>>> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.findAll(status)));
    }

    /**
     * API Lấy chi tiết thông tin giáo viên theo ID
     * Quyền hạn: ADMIN, STAFF, TEACHER
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Xem chi tiết giáo viên")
    public ResponseEntity<ApiResponse<TeacherResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.findById(id)));
    }

    /**
     * API Thêm mới một giáo viên
     * Quyền hạn: ADMIN, STAFF
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Thêm giáo viên mới", 
               description = "Cho phép quản trị viên hoặc giáo vụ tạo mới giáo viên. Mã giáo viên được tự động sinh dạng GVxxxx.")
    public ResponseEntity<ApiResponse<TeacherResponse>> create(@Valid @RequestBody TeacherRequest request) {
        TeacherResponse response = teacherService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới giáo viên thành công", response));
    }

    /**
     * API Cập nhật thông tin giáo viên theo ID
     * Quyền hạn: ADMIN, STAFF
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Cập nhật thông tin giáo viên")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(
            @PathVariable Long id, 
            @Valid @RequestBody TeacherRequest request) {
        TeacherResponse response = teacherService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin giáo viên thành công", response));
    }

    /**
     * API Xóa giáo viên (Xóa mềm - Soft Delete)
     * Quyền hạn: ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa giáo viên (Xóa mềm)", 
               description = "Thực hiện xóa mềm giáo viên bằng cách đổi cờ is_deleted = 1. Từ chối xóa nếu giáo viên đang có lịch dạy.")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giáo viên thành công", null));
    }

    /**
     * API Lấy lịch giảng dạy được phân công của giáo viên
     * Quyền hạn: ADMIN, STAFF, TEACHER
     */
    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Xem lịch dạy của giáo viên")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getSchedule(@PathVariable Long id) {
        List<TimetableEntryResponse> schedule = timetableService.getTimetable(id, null, null);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }
}
