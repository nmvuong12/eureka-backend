package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.request.TeacherRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.TimetableEntryResponse;
import com.eureka.timetabling.dto.response.TeacherResponse;
import com.eureka.timetabling.service.impl.TeacherService;
import com.eureka.timetabling.service.impl.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý giáo viên
 */
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Tag(name = "Giáo viên", description = "Quản lý thông tin giáo viên")
@SecurityRequirement(name = "bearerAuth")
public class TeacherController {

    private final TeacherService teacherService;
    private final TimetableService timetableService;

    @GetMapping
    @Operation(summary = "Danh sách giáo viên", description = "Lấy toàn bộ danh sách giáo viên, có thể lọc theo trạng thái")
    public ResponseEntity<ApiResponse<List<TeacherResponse>>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.findAll(status)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết giáo viên")
    public ResponseEntity<ApiResponse<TeacherResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(teacherService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo giáo viên mới")
    public ResponseEntity<ApiResponse<TeacherResponse>> create(@Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo giáo viên thành công", teacherService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật giáo viên")
    public ResponseEntity<ApiResponse<TeacherResponse>> update(
            @PathVariable Long id, @Valid @RequestBody TeacherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật giáo viên thành công", teacherService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa giáo viên")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa giáo viên thành công", null));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Lịch dạy của giáo viên", description = "Lấy tất cả buổi học được phân công cho giáo viên")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getSchedule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getTimetable(id, null, null)));
    }
}
