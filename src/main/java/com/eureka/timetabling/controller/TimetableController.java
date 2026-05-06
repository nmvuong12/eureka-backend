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
    @Operation(summary = "Xem thời khóa biểu", description = "Xem lịch có thể lọc theo giáo viên, lớp, hoặc phòng học")
    public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> getTimetable(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(timetableService.getTimetable(teacherId, classId, roomId)));
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
}
