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
import java.util.Map;
import java.util.HashMap;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

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
    private final NamedParameterJdbcTemplate jdbc;

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

    @GetMapping("/class-planning/dispatch")
    @Operation(summary = "Lấy danh sách buổi điều phối lịch giảng dạy theo bộ lọc nâng cao")
    public ResponseEntity<ApiResponse<List<DispatchEntryResponse>>> getDailyDispatch(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId) {
        
        java.time.LocalDate start = null;
        java.time.LocalDate end = null;

        try {
            if (startDate != null && !startDate.isBlank()) {
                start = java.time.LocalDate.parse(startDate.trim());
            }
            if (endDate != null && !endDate.isBlank()) {
                end = java.time.LocalDate.parse(endDate.trim());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Định dạng ngày bắt đầu hoặc ngày kết thúc không hợp lệ. Vui lòng sử dụng YYYY-MM-DD."));
        }

        if (start == null && end == null) {
            if (date != null && !date.isBlank()) {
                try {
                    start = java.time.LocalDate.parse(date.trim());
                    end = start;
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Định dạng ngày không hợp lệ. Vui lòng sử dụng YYYY-MM-DD."));
                }
            } else {
                start = java.time.LocalDate.now();
                end = start;
            }
        } else if (start == null) {
            start = end;
        } else if (end == null) {
            end = start;
        }

        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
        params.addValue("startDate", java.sql.Date.valueOf(start));
        params.addValue("endDate", java.sql.Date.valueOf(end));

        StringBuilder sqlBuilder = new StringBuilder("""
                SELECT l.id AS lesson_id, l.class_id, c.name AS class_name, l.lesson_index, l.required_skill,
                       la.teacher_id, t.full_name AS teacher_name,
                       la.room_id, r.name AS room_name,
                       la.timeslot_id, ts.day_of_week, ts.start_time, ts.end_time, ts.label AS timeslot_label,
                       la.session_date,
                       COALESCE(la.is_pinned, 0) AS pinned
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN teacher t ON la.teacher_id = t.id
                LEFT JOIN room r ON la.room_id = r.id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                WHERE la.session_date BETWEEN :startDate AND :endDate
                  AND c.is_deleted = 0
                  AND l.is_deleted = 0
                """);

        if (batchId != null) {
            sqlBuilder.append(" AND c.batch_id = :batchId");
            params.addValue("batchId", batchId);
        }
        if (classId != null) {
            sqlBuilder.append(" AND c.id = :classId");
            params.addValue("classId", classId);
        }
        if (teacherId != null) {
            sqlBuilder.append(" AND la.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }

        sqlBuilder.append(" ORDER BY la.session_date, ts.start_time, c.name, l.lesson_index");

        List<DispatchEntryResponse> list = jdbc.query(sqlBuilder.toString(), params, (rs, rowNum) -> {
            java.sql.Date sqlDate = rs.getDate("session_date");
            return DispatchEntryResponse.builder()
                    .lessonId(rs.getLong("lesson_id"))
                    .classId(rs.getLong("class_id"))
                    .className(rs.getString("class_name"))
                    .lessonIndex(rs.getInt("lesson_index"))
                    .requiredSkill(rs.getString("required_skill"))
                    .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
                    .teacherName(rs.getString("teacher_name"))
                    .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
                    .roomName(rs.getString("room_name"))
                    .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
                    .dayOfWeek(rs.getString("day_of_week"))
                    .startTime(rs.getString("start_time") != null ? rs.getString("start_time").substring(0, 5) : null)
                    .endTime(rs.getString("end_time") != null ? rs.getString("end_time").substring(0, 5) : null)
                    .timeslotLabel(rs.getString("timeslot_label"))
                    .sessionDate(sqlDate != null ? sqlDate.toLocalDate() : null)
                    .pinned(rs.getInt("pinned") == 1)
                    .build();
        });

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/class-planning/dispatch/{lessonId}")
    @Operation(summary = "Điều phối/Điều chỉnh lịch của một buổi học ngày cụ thể")
    public ResponseEntity<ApiResponse<Void>> adjustDispatch(
            @PathVariable Long lessonId,
            @RequestBody DispatchAdjustmentRequest request) {
        
        // Cập nhật và tự động ghim (lock) buổi học này
        String sql = """
                UPDATE lesson_assignment
                SET teacher_id = :teacherId,
                    room_id = :roomId,
                    timeslot_id = :timeslotId,
                    session_date = :sessionDate,
                    is_pinned = 1,
                    updated_at = NOW()
                WHERE lesson_id = :lessonId
                """;
        var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("lessonId", lessonId)
                .addValue("teacherId", request.getTeacherId())
                .addValue("roomId", request.getRoomId())
                .addValue("timeslotId", request.getTimeslotId())
                .addValue("sessionDate", request.getSessionDate() != null ? java.sql.Date.valueOf(request.getSessionDate()) : null);

        int updated = jdbc.update(sql, params);
        if (updated == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Không tìm thấy buổi phân công lịch với lessonId: " + lessonId));
        }

        return ResponseEntity.ok(ApiResponse.success("Điều phối ca học thành công (đã tự động khóa lịch)", null));
    }

    @PutMapping("/class-planning/dispatch/batch")
    @Operation(summary = "Điều phối lịch giảng dạy hàng loạt cho nhiều buổi học")
    public ResponseEntity<ApiResponse<Void>> adjustDispatchBatch(
            @Valid @RequestBody BatchDispatchAdjustmentRequest request) {
        
        if (request.getLessonIds() == null || request.getLessonIds().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Danh sách ID buổi học không được trống"));
        }
        
        boolean hasUpdates = (request.getTeacherId() != null || request.getRoomId() != null || 
                              request.getTimeslotId() != null || request.getSessionDate() != null);
        if (!hasUpdates) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Không có thông tin nào được thay đổi trong yêu cầu điều phối"));
        }

        // Lấy thông tin start_time, end_time của sourceTimeslotId nếu có đổi ca
        String startTime = null;
        String endTime = null;
        if (request.getTimeslotId() != null) {
            String tsSql = "SELECT start_time, end_time FROM timeslot WHERE id = :sourceId";
            List<java.util.Map<String, Object>> rows = jdbc.queryForList(tsSql, Map.of("sourceId", request.getTimeslotId()));
            if (!rows.isEmpty()) {
                startTime = rows.get(0).get("start_time").toString();
                endTime = rows.get(0).get("end_time").toString();
            }
        }

        int updatedCount = 0;
        // Duyệt từng buổi học để cập nhật động theo đúng ngày và ca học
        for (Long lessonId : request.getLessonIds()) {
            // Lấy thông tin ngày học hiện tại của buổi học
            String selectSql = "SELECT session_date FROM lesson_assignment WHERE lesson_id = :lessonId";
            List<java.util.Map<String, Object>> current = jdbc.queryForList(selectSql, Map.of("lessonId", lessonId));
            if (current.isEmpty()) continue;

            java.sql.Date originalDate = (java.sql.Date) current.get(0).get("session_date");
            java.time.LocalDate targetDate = (request.getSessionDate() != null) ? 
                    request.getSessionDate() : 
                    (originalDate != null ? originalDate.toLocalDate() : null);

            StringBuilder sql = new StringBuilder("UPDATE lesson_assignment SET is_pinned = 1, updated_at = NOW()");
            var params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
            params.addValue("lessonId", lessonId);

            if (request.getTeacherId() != null) {
                sql.append(", teacher_id = :teacherId");
                params.addValue("teacherId", request.getTeacherId());
            }
            if (request.getRoomId() != null) {
                sql.append(", room_id = :roomId");
                params.addValue("roomId", request.getRoomId());
            }
            if (request.getSessionDate() != null) {
                sql.append(", session_date = :sessionDate");
                params.addValue("sessionDate", java.sql.Date.valueOf(request.getSessionDate()));
            }

            if (request.getTimeslotId() != null && targetDate != null && startTime != null && endTime != null) {
                String dayOfWeek = targetDate.getDayOfWeek().toString();
                // Tìm ca học khớp với thứ của ngày học đó
                String findTsSql = "SELECT id FROM timeslot WHERE day_of_week = :dayOfWeek AND start_time = :startTime AND end_time = :endTime LIMIT 1";
                List<java.util.Map<String, Object>> match = jdbc.queryForList(findTsSql, Map.of(
                        "dayOfWeek", dayOfWeek,
                        "startTime", startTime,
                        "endTime", endTime
                ));
                Long resolvedTsId = match.isEmpty() ? request.getTimeslotId() : ((Number) match.get(0).get("id")).longValue();
                
                sql.append(", timeslot_id = :timeslotId");
                params.addValue("timeslotId", resolvedTsId);
            } else if (request.getTimeslotId() != null) {
                sql.append(", timeslot_id = :timeslotId");
                params.addValue("timeslotId", request.getTimeslotId());
            }

            sql.append(" WHERE lesson_id = :lessonId");
            updatedCount += jdbc.update(sql.toString(), params);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Điều phối hàng loạt ca học thành công (" + updatedCount + " buổi học đã được ghim khóa lịch)", null));
    }
}
