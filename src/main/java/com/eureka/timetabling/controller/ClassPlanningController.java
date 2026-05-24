package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.ClassPlanningLog;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.SchoolClass;
import com.eureka.timetabling.domain.SchedulePattern;
import com.eureka.timetabling.dto.request.*;
import com.eureka.timetabling.dto.response.*;
import com.eureka.timetabling.repository.ClassPlanningLogRepository;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.service.CapacityCalculationService;
import com.eureka.timetabling.service.ClassPlanningService;
import com.eureka.timetabling.service.SchedulePatternService;
import com.eureka.timetabling.service.impl.ClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý việc Lập kế hoạch mở lớp (Rolling Scheduling) & Lớp học.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Lập kế hoạch mở lớp", description = "API cho quy trình Rolling Scheduling và quản lý lớp học")
public class ClassPlanningController {

    private final ClassPlanningService classPlanningService;
    private final CapacityCalculationService capacityCalculationService;
    private final ClassService classService;
    private final ClassRepository classRepository;
    private final ClassPlanningLogRepository classPlanningLogRepository;
    private final SchedulePatternService schedulePatternService;

    // ==================== ROLLING SCHEDULING ENDPOINTS ====================

    @GetMapping("/class-planning/patterns")
    @Operation(summary = "Danh sách mẫu lịch học chuẩn")
    public ResponseEntity<ApiResponse<List<SchedulePattern>>> getPatterns() {
        return ResponseEntity.ok(ApiResponse.success(schedulePatternService.findAllActive()));
    }

    @GetMapping("/class-planning/capacity")
    @Operation(summary = "Lấy Capacity Dashboard theo Khóa học (Resource-based)")
    public ResponseEntity<ApiResponse<List<CapacityDashboardItem>>> getCapacityDashboard(
            @RequestParam Long courseId) {
        List<CapacityDashboardItem> items = capacityCalculationService.calculateDashboard(courseId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping("/class-planning/generate")
    @Operation(summary = "Sinh lớp nháp (DRAFT) hàng loạt")
    public ResponseEntity<ApiResponse<List<SchoolClass>>> generateDraftClasses(
            @Valid @RequestBody GenerateClassRequest request) {
        List<SchoolClass> classes = classPlanningService.generateDraftClasses(
                request.getBatchId(), request.getPatternId(), request.getCount());
        return ResponseEntity.ok(ApiResponse.success("Sinh lớp học nháp thành công", classes));
    }

    @PostMapping("/class-planning/smart-generate")
    @Operation(summary = "Lập kế hoạch tự động (Phù hợp nhất)")
    public ResponseEntity<ApiResponse<List<SchoolClass>>> smartGenerate(
            @RequestParam Long batchId) {
        List<SchoolClass> classes = classPlanningService.smartGenerate(batchId);
        return ResponseEntity.ok(ApiResponse.success("Tự động lập kế hoạch và sinh lớp nháp thành công", classes));
    }

    @PostMapping("/class-planning/batches/{batchId}/activate-enrollment")
    @Operation(summary = "Kích hoạt tuyển sinh (DRAFT -> ENROLLING) cho toàn bộ lớp của Batch")
    public ResponseEntity<ApiResponse<Void>> activateEnrollment(@PathVariable Long batchId) {
        classPlanningService.activateEnrollment(batchId);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt tuyển sinh lớp học thành công", null));
    }

    @PutMapping("/class-planning/classes/{id}/student-count")
    @Operation(summary = "Cập nhật sĩ số học sinh tuyển sinh thực tế cho lớp")
    public ResponseEntity<ApiResponse<Void>> updateStudentCount(
            @PathVariable Long id,
            @Valid @RequestBody StudentCountRequest request) {
        classPlanningService.updateStudentCount(id, request.getStudentCount());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sĩ số học viên thành công", null));
    }

    @PostMapping("/class-planning/batches/{batchId}/rebalance")
    @Operation(summary = "Chạy thuật toán cân bằng sĩ số cho cùng batch và pattern")
    public ResponseEntity<ApiResponse<RebalanceResult>> rebalanceClasses(
            @PathVariable Long batchId,
            @RequestParam Long patternId) {
        RebalanceResult result = classPlanningService.rebalanceClasses(batchId, patternId);
        return ResponseEntity.ok(ApiResponse.success("Thực hiện cân bằng sĩ số thành công", result));
    }

    @PostMapping("/class-planning/classes/{id}/open")
    @Operation(summary = "Mở lớp học chính thức thủ công")
    public ResponseEntity<ApiResponse<Void>> openClass(@PathVariable Long id) {
        classPlanningService.openClass(id);
        return ResponseEntity.ok(ApiResponse.success("Mở lớp học thành công", null));
    }

    @PostMapping("/class-planning/classes/{id}/force-open")
    @Operation(summary = "Mở lớp học cưỡng bức (bỏ qua sĩ số tối thiểu)")
    public ResponseEntity<ApiResponse<Void>> forceOpenClass(@PathVariable Long id) {
        classPlanningService.forceOpenClass(id);
        return ResponseEntity.ok(ApiResponse.success("Buộc mở lớp học thành công", null));
    }

    @PostMapping("/class-planning/classes/{id}/cancel")
    @Operation(summary = "Hủy lớp học kèm lý do")
    public ResponseEntity<ApiResponse<Void>> cancelClass(
            @PathVariable Long id,
            @RequestBody(required = false) CancelClassRequest request) {
        String reason = request != null ? request.getReason() : "Hủy không lý do";
        classPlanningService.cancelClass(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy lớp học thành công", null));
    }

    @PostMapping("/class-planning/merge")
    @Operation(summary = "Gộp các lớp ít học viên vào một lớp đích")
    public ResponseEntity<ApiResponse<MergeResult>> mergeClasses(
            @Valid @RequestBody MergeClassRequest request) {
        MergeResult result = classPlanningService.mergeClasses(request.getSourceClassIds(), request.getTargetClassId());
        return ResponseEntity.ok(ApiResponse.success("Gộp lớp thành công", result));
    }

    @PutMapping("/class-planning/classes/{id}")
    @Operation(summary = "Cập nhật thông tin chi tiết lớp học")
    public ResponseEntity<ApiResponse<SchoolClass>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody ClassUpdateRequest request) {
        SchoolClass updated = classPlanningService.updateClass(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lớp học thành công", updated));
    }

    @DeleteMapping("/class-planning/classes/{id}")
    @Operation(summary = "Xóa lớp học")
    public ResponseEntity<ApiResponse<Void>> deleteClass(@PathVariable Long id) {
        classPlanningService.deleteClass(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa lớp học thành công", null));
    }

    @GetMapping("/class-planning/classes/{id}/log")
    @Operation(summary = "Xem lịch sử thay đổi/audit log của lớp học")
    public ResponseEntity<ApiResponse<List<ClassPlanningLog>>> getPlanningLogs(@PathVariable Long id) {
        List<ClassPlanningLog> logs = classPlanningLogRepository.findByClassId(id);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/class-planning/classes")
    @Operation(summary = "Danh sách lớp học theo bộ lọc nâng cao")
    public ResponseEntity<ApiResponse<List<SchoolClass>>> getClassesFiltered(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String patternId,
            @RequestParam(required = false) String teacherId) {
        Long bId = parseId(batchId);
        Long pId = parseId(patternId);
        Long tId = parseId(teacherId);
        String st = ("all".equalsIgnoreCase(status) || status == null || status.isBlank()) ? null : status;
        List<SchoolClass> list = classRepository.findByFilters(bId, st, pId, tId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    private Long parseId(String idStr) {
        if (idStr == null || idStr.isBlank() || "all".equalsIgnoreCase(idStr)) {
            return null;
        }
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== LEGACY COMPATIBILITY ENDPOINTS ====================

    @GetMapping("/classes")
    @Operation(summary = "Danh sách tất cả lớp học (Legacy)")
    public ResponseEntity<ApiResponse<List<SchoolClass>>> getAllClasses() {
        return ResponseEntity.ok(ApiResponse.success(classService.findAll()));
    }

    @PostMapping("/classes")
    @Operation(summary = "Tạo lớp học và tự động sinh buổi học (Legacy)")
    public ResponseEntity<ApiResponse<SchoolClass>> createClass(
            @Valid @RequestBody ClassRequest request) {
        SchoolClass clazz = classService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo lớp học thành công", clazz));
    }

    @GetMapping("/classes/{id}/lessons")
    @Operation(summary = "Danh sách buổi học của lớp (Legacy)")
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessons(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getLessons(id)));
    }
}
