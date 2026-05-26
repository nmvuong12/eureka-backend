package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.dto.request.LessonAssignmentRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.TimetableEntryResponse;
import com.eureka.timetabling.repository.TeacherRepository;
import com.eureka.timetabling.service.impl.ClassService;
import com.eureka.timetabling.service.impl.TimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API Quản lý Thời khóa biểu
 */
@RestController
@RequestMapping("/timetable")
@RequiredArgsConstructor
@Tag(name = "Thời khóa biểu", description = "Lập lịch tự động và quản lý thời khóa biểu")
@SecurityRequirement(name = "bearerAuth")
public class TimetableController {

    private final TimetableService timetableService;
    private final ClassService classService;
    private final TeacherRepository teacherRepository;

    @PostMapping("/solve")
    @Operation(summary = "Chạy thuật toán xếp lịch tự động", description = "Chạy Timefold Solver bất đồng bộ")
    public ResponseEntity<ApiResponse<Map<String, String>>> solve() {
        String jobId = UUID.randomUUID().toString();
        timetableService.solveAsync(jobId);
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu xếp lịch", Map.of("jobId", jobId)));
    }

    @GetMapping("/solve/status/{jobId}")
    @Operation(summary = "Kiểm tra trạng thái xếp lịch", description = "Trả về trạng thái: SOLVING, DONE:score, ERROR:msg")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSolveStatus(@PathVariable String jobId) {
        String status = timetableService.getSolverStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("jobId", jobId, "status", status)));
    }

    @GetMapping
    @Operation(summary = "Xem thời khóa biểu chuẩn", description = "Xem lịch học chuẩn hàng tuần có thể lọc theo đợt khai giảng, giáo viên, lớp, hoặc phòng học")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getTimetable(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long batchId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getTimetable(teacherId, classId, roomId, batchId)));
    }

    @GetMapping("/weekly-calendar")
    @Operation(summary = "Xem lịch học thực tế theo tuần dương lịch", description = "Xem lịch học thực tế từ ngày bắt đầu đến ngày kết thúc có thể lọc theo đợt khai giảng, giáo viên, lớp, hoặc phòng học")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getWeeklyCalendar(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long batchId) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate.trim());
        java.time.LocalDate end = java.time.LocalDate.parse(endDate.trim());
        return ResponseEntity.ok(ApiResponse.success(timetableService.getWeeklyCalendar(start, end, teacherId, classId, roomId, batchId)));
    }

    @PutMapping("/lesson/{id}")
    @Operation(summary = "Chỉnh sửa phân công thủ công", description = "Đổi giáo viên, phòng hoặc ca cho 1 buổi học cụ thể")
    public ResponseEntity<ApiResponse<Void>> updateLessonAssignment(
            @PathVariable Long id, @RequestBody LessonAssignmentRequest request) {
        classService.updateLessonAssignment(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật phân công thành công", null));
    }

    @GetMapping("/suggest-teacher")
    @Operation(summary = "Gợi ý giáo viên thay thế", description = "Tìm giáo viên có kỹ năng phù hợp và trống lịch vào ca đó")
    public ResponseEntity<ApiResponse<List<Teacher>>> suggestTeacher(
            @RequestParam String skillCode,
            @RequestParam Long timeslotId,
            @RequestParam Long excludeTeacherId) {
        List<Teacher> suggests = teacherRepository.findSuitableSubstitutes(skillCode, timeslotId, excludeTeacherId);
        return ResponseEntity.ok(ApiResponse.success(suggests));
    }

    @PostMapping("/lock")
    @Operation(summary = "Khóa thời khóa biểu", description = "Khóa và ghim tất cả các buổi học, chuyển trạng thái lớp sang STUDYING")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lockTimetable(
            @RequestParam(required = false) Long batchId) {
        int count = timetableService.lockTimetable(batchId);
        return ResponseEntity.ok(ApiResponse.success("Khóa thời khóa biểu thành công", Map.of("lockedSessions", count)));
    }
}
