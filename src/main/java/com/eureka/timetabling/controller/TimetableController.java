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

import com.eureka.timetabling.dto.request.BatchValidateAssignmentRequest;
import com.eureka.timetabling.domain.TeacherType;
import com.eureka.timetabling.domain.WorkingStatus;
import com.eureka.timetabling.domain.Gender;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

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
    private final NamedParameterJdbcTemplate jdbc;

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
            @RequestParam(required = false) Long excludeTeacherId,
            @RequestParam(required = false) String sessionDate) {
        java.time.LocalDate date = null;
        if (sessionDate != null && !sessionDate.isBlank()) {
            try {
                date = java.time.LocalDate.parse(sessionDate.trim());
            } catch (Exception ignored) {}
        }
        Long exTeacherId = excludeTeacherId != null ? excludeTeacherId : 0L;
        List<Teacher> suggests = teacherRepository.findSuitableSubstitutes(skillCode, timeslotId, exTeacherId, date);
        return ResponseEntity.ok(ApiResponse.success(suggests));
    }

    @GetMapping("/validate-assignment")
    @Operation(summary = "Xác thực xung đột phân công", description = "Kiểm tra trùng lịch cho giáo viên, phòng học vào ngày và ca cụ thể")
    public ResponseEntity<ApiResponse<Map<String, String>>> validateAssignment(
            @RequestParam Long lessonId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long roomId,
            @RequestParam Long timeslotId,
            @RequestParam String sessionDate) {
        
        java.time.LocalDate date;
        try {
            date = java.time.LocalDate.parse(sessionDate.trim());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Định dạng ngày không hợp lệ. Vui lòng sử dụng YYYY-MM-DD."));
        }

        java.util.Map<String, String> conflicts = new java.util.HashMap<>();

        // 1. Kiểm tra trùng giáo viên
        if (teacherId != null && teacherId > 0) {
            // Check trùng lịch dạy thực tế
            String teacherSql = """
                    SELECT c.name AS class_name, l.lesson_index
                    FROM lesson_assignment la
                    INNER JOIN lesson l ON la.lesson_id = l.id
                    INNER JOIN class c ON l.class_id = c.id
                    WHERE la.teacher_id = :teacherId
                      AND la.timeslot_id = :timeslotId
                      AND la.session_date = :sessionDate
                      AND la.lesson_id != :lessonId
                      AND la.is_deleted = 0 AND l.is_deleted = 0 AND c.is_deleted = 0
                    LIMIT 1
                    """;
            var teacherParams = new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("timeslotId", timeslotId)
                    .addValue("sessionDate", java.sql.Date.valueOf(date))
                    .addValue("lessonId", lessonId);
            List<java.util.Map<String, Object>> teacherConflicts = jdbc.queryForList(teacherSql, teacherParams);
            if (!teacherConflicts.isEmpty()) {
                String className = (String) teacherConflicts.get(0).get("class_name");
                int index = ((Number) teacherConflicts.get(0).get("lesson_index")).intValue();
                conflicts.put("teacherConflict", String.format("Giáo viên đã bận dạy lớp %s (Buổi #%d) tại ca này.", className, index));
            }

            // Check đơn nghỉ phép
            String leaveSql = """
                    SELECT from_date, to_date, reason
                    FROM leave_request
                    WHERE teacher_id = :teacherId
                      AND status = 'APPROVED'
                      AND :sessionDate BETWEEN from_date AND to_date
                      AND is_deleted = 0
                    LIMIT 1
                    """;
            List<java.util.Map<String, Object>> leaveConflicts = jdbc.queryForList(leaveSql, new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("sessionDate", java.sql.Date.valueOf(date)));
            if (!leaveConflicts.isEmpty()) {
                java.sql.Date from = (java.sql.Date) leaveConflicts.get(0).get("from_date");
                java.sql.Date to = (java.sql.Date) leaveConflicts.get(0).get("to_date");
                conflicts.put("teacherLeave", String.format("Giáo viên đã được duyệt nghỉ phép từ ngày %s đến %s.", from, to));
            }

            // Check bận cố định
            String unavailSql = """
                    SELECT reason FROM teacher_unavailable
                    WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId
                    LIMIT 1
                    """;
            List<java.util.Map<String, Object>> unavailList = jdbc.queryForList(unavailSql, new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("timeslotId", timeslotId));
            if (!unavailList.isEmpty()) {
                String reason = (String) unavailList.get(0).get("reason");
                String reasonStr = (reason != null && !reason.isBlank()) ? " (" + reason + ")" : "";
                conflicts.put("teacherUnavailable", "Giáo viên đã đăng ký lịch bận cố định ca học này" + reasonStr);
            }
        }

        // 2. Kiểm tra trùng phòng học
        if (roomId != null && roomId > 0) {
            String roomSql = """
                    SELECT c.name AS class_name, l.lesson_index
                    FROM lesson_assignment la
                    INNER JOIN lesson l ON la.lesson_id = l.id
                    INNER JOIN class c ON l.class_id = c.id
                    WHERE la.room_id = :roomId
                      AND la.timeslot_id = :timeslotId
                      AND la.session_date = :sessionDate
                      AND la.lesson_id != :lessonId
                      AND la.is_deleted = 0 AND l.is_deleted = 0 AND c.is_deleted = 0
                    LIMIT 1
                    """;
            var roomParams = new MapSqlParameterSource()
                    .addValue("roomId", roomId)
                    .addValue("timeslotId", timeslotId)
                    .addValue("sessionDate", java.sql.Date.valueOf(date))
                    .addValue("lessonId", lessonId);
            List<java.util.Map<String, Object>> roomConflicts = jdbc.queryForList(roomSql, roomParams);
            if (!roomConflicts.isEmpty()) {
                String className = (String) roomConflicts.get(0).get("class_name");
                int index = ((Number) roomConflicts.get(0).get("lesson_index")).intValue();
                conflicts.put("roomConflict", String.format("Phòng học đã bị chiếm bởi lớp %s (Buổi #%d) tại ca này.", className, index));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(conflicts));
    }

    @PostMapping("/lock")
    @Operation(summary = "Khóa thời khóa biểu", description = "Khóa và ghim tất cả các buổi học, chuyển trạng thái lớp sang STUDYING")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lockTimetable(
            @RequestParam(required = false) Long batchId) {
        int count = timetableService.lockTimetable(batchId);
        return ResponseEntity.ok(ApiResponse.success("Khóa thời khóa biểu thành công", Map.of("lockedSessions", count)));
    }

    @PostMapping("/suggest-teacher/batch")
    @Operation(summary = "Gợi ý giáo viên tối ưu cho loạt buổi học", description = "Tìm giáo viên trống lịch và đủ trình độ giảng dạy tất cả các buổi học được chọn")
    public ResponseEntity<ApiResponse<List<Teacher>>> suggestTeacherBatch(
            @Valid @RequestBody BatchValidateAssignmentRequest request) {
        
        if (request.getLessonIds() == null || request.getLessonIds().isEmpty() || request.getTimeslotId() == null) {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }

        // 1. Lấy thông tin start_time, end_time của request.getTimeslotId()
        String startTime = null;
        String endTime = null;
        String tsSql = "SELECT start_time, end_time FROM timeslot WHERE id = :sourceId";
        List<java.util.Map<String, Object>> tsRow = jdbc.queryForList(tsSql, Map.of("sourceId", request.getTimeslotId()));
        if (!tsRow.isEmpty()) {
            startTime = tsRow.get(0).get("start_time").toString();
            endTime = tsRow.get(0).get("end_time").toString();
        } else {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }

        // 2. Lấy tất cả kỹ năng yêu cầu của các buổi học đã chọn
        String skillsSql = "SELECT DISTINCT required_skill FROM lesson WHERE id IN (:lessonIds) AND is_deleted = 0";
        List<String> requiredSkills = jdbc.query(skillsSql, 
                new MapSqlParameterSource("lessonIds", request.getLessonIds()), 
                (rs, rowNum) -> rs.getString("required_skill"));

        if (requiredSkills.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
        }

        // 3. Lấy thông tin ngày học hiện tại của từng buổi học
        String selectSql = "SELECT lesson_id, session_date FROM lesson_assignment WHERE lesson_id IN (:lessonIds)";
        List<java.util.Map<String, Object>> currentAssignments = jdbc.queryForList(
                selectSql, new MapSqlParameterSource("lessonIds", request.getLessonIds()));

        // Xây dựng danh sách các cặp (Ngày học, Ca học) cần dạy
        java.util.List<java.util.Map<String, Object>> slotsToTeach = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> current : currentAssignments) {
            java.sql.Date originalDate = (java.sql.Date) current.get("session_date");
            java.sql.Date targetDate = (request.getSessionDate() != null) ? 
                    java.sql.Date.valueOf(request.getSessionDate()) : 
                    originalDate;
            if (targetDate == null) continue;

            String dayOfWeek = targetDate.toLocalDate().getDayOfWeek().toString();
            // Tìm ca học tương ứng với thứ của ngày đó
            String findTsSql = "SELECT id FROM timeslot WHERE day_of_week = :dayOfWeek AND start_time = :startTime AND end_time = :endTime LIMIT 1";
            List<java.util.Map<String, Object>> match = jdbc.queryForList(findTsSql, Map.of(
                    "dayOfWeek", dayOfWeek,
                    "startTime", startTime,
                    "endTime", endTime
            ));
            Long resolvedTsId = match.isEmpty() ? request.getTimeslotId() : ((Number) match.get(0).get("id")).longValue();

            java.util.Map<String, Object> slot = new java.util.HashMap<>();
            slot.put("lessonId", current.get("lesson_id"));
            slot.put("sessionDate", targetDate);
            slot.put("timeslotId", resolvedTsId);
            slotsToTeach.add(slot);
        }

        // 4. Tìm các giáo viên thỏa mãn kỹ năng chuyên môn
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT t.id, t.teacher_code, t.teacher_type, t.full_name, t.date_of_birth,
                                t.gender, t.address, t.email, t.phone, t.skills, t.certificate_file,
                                t.profile_file, t.working_status, t.is_deleted, t.created_date, t.modified_date
                FROM teacher t
                """);
        
        var params = new MapSqlParameterSource();

        int skillIndex = 0;
        for (String skillCode : requiredSkills) {
            skillIndex++;
            String skillAlias = "ts" + skillIndex;
            String sAlias = "s" + skillIndex;
            
            // Kiểm tra xem kỹ năng này có phân cấp không
            String checkSql = "SELECT skill_group, level_rank FROM skill WHERE skill_code = :skillCode_" + skillIndex + " AND is_deleted = 0";
            var skillCheckParams = new MapSqlParameterSource("skillCode_" + skillIndex, skillCode);
            List<java.util.Map<String, Object>> skillInfo = jdbc.queryForList(checkSql, skillCheckParams);
            
            if (!skillInfo.isEmpty() && skillInfo.get(0).get("skill_group") != null && skillInfo.get(0).get("level_rank") != null) {
                String group = (String) skillInfo.get(0).get("skill_group");
                int rank = ((Number) skillInfo.get(0).get("level_rank")).intValue();
                
                sql.append(" INNER JOIN teacher_skill ").append(skillAlias).append(" ON t.id = ").append(skillAlias).append(".teacher_id ")
                   .append(" INNER JOIN skill ").append(sAlias).append(" ON ").append(skillAlias).append(".skill_code = ").append(sAlias).append(".skill_code AND ")
                   .append(sAlias).append(".skill_group = :skillGroup_").append(skillIndex)
                   .append(" AND ").append(sAlias).append(".level_rank >= :levelRank_").append(skillIndex).append(" ");
                
                params.addValue("skillGroup_" + skillIndex, group);
                params.addValue("levelRank_" + skillIndex, rank);
            } else {
                sql.append(" INNER JOIN teacher_skill ").append(skillAlias).append(" ON t.id = ").append(skillAlias).append(".teacher_id AND ")
                   .append(skillAlias).append(".skill_code = :skillCode_").append(skillIndex).append(" ");
                params.addValue("skillCode_" + skillIndex, skillCode);
            }
        }

        sql.append(" WHERE t.working_status = 'ACTIVE' AND t.is_deleted = 0 ORDER BY t.full_name ");

        List<Teacher> candidates = jdbc.query(sql.toString(), params, (rs, rowNum) -> {
            String statusStr = rs.getString("working_status");
            String typeStr = rs.getString("teacher_type");
            String genderStr = rs.getString("gender");
            
            return Teacher.builder()
                    .id(rs.getLong("id"))
                    .teacherCode(rs.getString("teacher_code"))
                    .teacherType(typeStr != null ? TeacherType.valueOf(typeStr) : null)
                    .fullName(rs.getString("full_name"))
                    .email(rs.getString("email"))
                    .phone(rs.getString("phone"))
                    .skills(rs.getString("skills"))
                    .gender(genderStr != null ? Gender.valueOf(genderStr) : null)
                    .workingStatus(statusStr != null ? WorkingStatus.valueOf(statusStr) : null)
                    .build();
        });

        // 5. Lọc ra các giáo viên thực sự rảnh ở tất cả các slot cần dạy
        java.util.List<Teacher> freeTeachers = new java.util.ArrayList<>();
        for (Teacher teacher : candidates) {
            boolean isFreeAll = true;
            for (var slot : slotsToTeach) {
                Long lessonId = (Long) slot.get("lessonId");
                java.sql.Date date = (java.sql.Date) slot.get("sessionDate");
                Long timeslotId = (Long) slot.get("timeslotId");

                // Check trùng lịch dạy thực tế trong DB
                String checkBusySql = """
                        SELECT 1 FROM lesson_assignment
                        WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId AND session_date = :sessionDate AND lesson_id != :lessonId AND is_deleted = 0
                        LIMIT 1
                        """;
                List<Integer> busy = jdbc.query(checkBusySql, new MapSqlParameterSource()
                        .addValue("teacherId", teacher.getId())
                        .addValue("timeslotId", timeslotId)
                        .addValue("sessionDate", date)
                        .addValue("lessonId", lessonId),
                        (rs, rowNum) -> 1);
                if (!busy.isEmpty()) {
                    isFreeAll = false;
                    break;
                }

                // Check lịch bận cố định
                String checkUnavailSql = "SELECT 1 FROM teacher_unavailable WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId LIMIT 1";
                List<Integer> unavail = jdbc.query(checkUnavailSql, new MapSqlParameterSource()
                        .addValue("teacherId", teacher.getId())
                        .addValue("timeslotId", timeslotId),
                        (rs, rowNum) -> 1);
                if (!unavail.isEmpty()) {
                    isFreeAll = false;
                    break;
                }

                // Check nghỉ phép
                String checkLeaveSql = """
                        SELECT 1 FROM leave_request
                        WHERE teacher_id = :teacherId AND status = 'APPROVED' AND :sessionDate BETWEEN from_date AND to_date AND is_deleted = 0
                        LIMIT 1
                        """;
                List<Integer> leave = jdbc.query(checkLeaveSql, new MapSqlParameterSource()
                        .addValue("teacherId", teacher.getId())
                        .addValue("sessionDate", date),
                        (rs, rowNum) -> 1);
                if (!leave.isEmpty()) {
                    isFreeAll = false;
                    break;
                }
            }
            if (isFreeAll) {
                freeTeachers.add(teacher);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(freeTeachers));
    }

    @PostMapping("/validate-assignment/batch")
    @Operation(summary = "Xác thực xung đột phân công hàng loạt", description = "Kiểm tra trùng lịch cho giáo viên, phòng học hàng loạt")
    public ResponseEntity<ApiResponse<Map<String, String>>> validateAssignmentBatch(
            @Valid @RequestBody BatchValidateAssignmentRequest request) {
        
        if (request.getLessonIds() == null || request.getLessonIds().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyMap()));
        }

        java.util.Map<String, String> conflicts = new java.util.HashMap<>();
        
        // Lấy thông tin hiện tại của từng buổi học để điền các trường null
        String selectSql = """
                SELECT lesson_id, teacher_id, room_id, timeslot_id, session_date
                FROM lesson_assignment
                WHERE lesson_id IN (:lessonIds)
                """;
        List<java.util.Map<String, Object>> currentAssignments = jdbc.queryForList(
                selectSql, new MapSqlParameterSource("lessonIds", request.getLessonIds()));

        // Lấy thông tin start_time, end_time của request.getTimeslotId() nếu có đổi ca
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
        
        // Ánh xạ thành danh sách các lesson với thông tin phân công dự kiến
        java.util.List<java.util.Map<String, Object>> proposedAssignments = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> current : currentAssignments) {
            java.util.Map<String, Object> proposed = new java.util.HashMap<>(current);
            if (request.getTeacherId() != null) proposed.put("teacher_id", request.getTeacherId());
            if (request.getRoomId() != null) proposed.put("room_id", request.getRoomId());
            
            java.sql.Date originalDate = (java.sql.Date) current.get("session_date");
            java.sql.Date targetDate = (request.getSessionDate() != null) ? 
                    java.sql.Date.valueOf(request.getSessionDate()) : 
                    originalDate;
            
            if (request.getSessionDate() != null) {
                proposed.put("session_date", targetDate);
            }
            
            if (request.getTimeslotId() != null) {
                if (targetDate != null && startTime != null && endTime != null) {
                    java.time.LocalDate localDate = targetDate.toLocalDate();
                    String dayOfWeek = localDate.getDayOfWeek().toString();
                    
                    String findTsSql = "SELECT id FROM timeslot WHERE day_of_week = :dayOfWeek AND start_time = :startTime AND end_time = :endTime LIMIT 1";
                    List<java.util.Map<String, Object>> match = jdbc.queryForList(findTsSql, Map.of(
                            "dayOfWeek", dayOfWeek,
                            "startTime", startTime,
                            "endTime", endTime
                    ));
                    Long resolvedTsId = match.isEmpty() ? request.getTimeslotId() : ((Number) match.get(0).get("id")).longValue();
                    proposed.put("timeslot_id", resolvedTsId);
                } else {
                    proposed.put("timeslot_id", request.getTimeslotId());
                }
            }
            
            proposedAssignments.add(proposed);
        }
        
        // 1. Kiểm tra Trùng lịch nội bộ (Self-Conflicts) trong batch đề xuất
        for (int i = 0; i < proposedAssignments.size(); i++) {
            var a1 = proposedAssignments.get(i);
            Long l1 = ((Number) a1.get("lesson_id")).longValue();
            Long t1 = a1.get("teacher_id") != null ? ((Number) a1.get("teacher_id")).longValue() : null;
            Long r1 = a1.get("room_id") != null ? ((Number) a1.get("room_id")).longValue() : null;
            Long ts1 = a1.get("timeslot_id") != null ? ((Number) a1.get("timeslot_id")).longValue() : null;
            java.sql.Date d1 = (java.sql.Date) a1.get("session_date");
            
            if (d1 == null || ts1 == null) continue;
            
            for (int j = i + 1; j < proposedAssignments.size(); j++) {
                var a2 = proposedAssignments.get(j);
                Long l2 = ((Number) a2.get("lesson_id")).longValue();
                Long t2 = a2.get("teacher_id") != null ? ((Number) a2.get("teacher_id")).longValue() : null;
                Long r2 = a2.get("room_id") != null ? ((Number) a2.get("room_id")).longValue() : null;
                Long ts2 = a2.get("timeslot_id") != null ? ((Number) a2.get("timeslot_id")).longValue() : null;
                java.sql.Date d2 = (java.sql.Date) a2.get("session_date");
                
                if (d2 == null || ts2 == null) continue;
                
                if (d1.equals(d2) && ts1.equals(ts2)) {
                    // Kiểm tra giáo viên
                    if (t1 != null && t1.equals(t2)) {
                        conflicts.put("batchTeacherConflict", "Xung đột nội bộ: Nhiều buổi học trong danh sách chọn được xếp cho cùng một giáo viên ở cùng ca/ngày.");
                    }
                    // Kiểm tra phòng học
                    if (r1 != null && r1.equals(r2)) {
                        conflicts.put("batchRoomConflict", "Xung đột nội bộ: Nhiều buổi học trong danh sách chọn được xếp cho cùng một phòng học ở cùng ca/ngày.");
                    }
                }
            }
        }
        
        // 2. Kiểm tra Xung đột với Database (các buổi học ngoài batch)
        for (var proposed : proposedAssignments) {
            Long lessonId = ((Number) proposed.get("lesson_id")).longValue();
            Long teacherId = proposed.get("teacher_id") != null ? ((Number) proposed.get("teacher_id")).longValue() : null;
            Long roomId = proposed.get("room_id") != null ? ((Number) proposed.get("room_id")).longValue() : null;
            Long timeslotId = proposed.get("timeslot_id") != null ? ((Number) proposed.get("timeslot_id")).longValue() : null;
            java.sql.Date date = (java.sql.Date) proposed.get("session_date");
            
            if (date == null || timeslotId == null) continue;
            
            // Check trùng giáo viên trong DB (ngoại trừ các lessonId thuộc batch)
            if (teacherId != null && teacherId > 0) {
                String teacherSql = """
                        SELECT c.name AS class_name, l.lesson_index
                        FROM lesson_assignment la
                        INNER JOIN lesson l ON la.lesson_id = l.id
                        INNER JOIN class c ON l.class_id = c.id
                        WHERE la.teacher_id = :teacherId
                          AND la.timeslot_id = :timeslotId
                          AND la.session_date = :sessionDate
                          AND la.lesson_id NOT IN (:lessonIds)
                          AND la.is_deleted = 0 AND l.is_deleted = 0 AND c.is_deleted = 0
                        LIMIT 1
                        """;
                var teacherParams = new MapSqlParameterSource()
                        .addValue("teacherId", teacherId)
                        .addValue("timeslotId", timeslotId)
                        .addValue("sessionDate", date)
                        .addValue("lessonIds", request.getLessonIds());
                List<java.util.Map<String, Object>> teacherConflicts = jdbc.queryForList(teacherSql, teacherParams);
                if (!teacherConflicts.isEmpty()) {
                     String className = (String) teacherConflicts.get(0).get("class_name");
                     int index = ((Number) teacherConflicts.get(0).get("lesson_index")).intValue();
                     conflicts.put("teacherConflict", String.format("Giáo viên đã bận dạy lớp %s (Buổi #%d) tại ca học này.", className, index));
                }
 
                // Check đơn nghỉ phép
                String leaveSql = """
                        SELECT from_date, to_date, reason
                        FROM leave_request
                        WHERE teacher_id = :teacherId
                          AND status = 'APPROVED'
                          AND :sessionDate BETWEEN from_date AND to_date
                          AND is_deleted = 0
                        LIMIT 1
                        """;
                List<java.util.Map<String, Object>> leaveConflicts = jdbc.queryForList(leaveSql, new MapSqlParameterSource()
                        .addValue("teacherId", teacherId)
                        .addValue("sessionDate", date));
                if (!leaveConflicts.isEmpty()) {
                     java.sql.Date from = (java.sql.Date) leaveConflicts.get(0).get("from_date");
                     java.sql.Date to = (java.sql.Date) leaveConflicts.get(0).get("to_date");
                     conflicts.put("teacherLeave", String.format("Giáo viên đã được duyệt nghỉ phép từ ngày %s đến %s.", from, to));
                }
 
                // Check bận cố định
                String unavailSql = """
                        SELECT reason FROM teacher_unavailable
                        WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId
                        LIMIT 1
                        """;
                List<java.util.Map<String, Object>> unavailList = jdbc.queryForList(unavailSql, new MapSqlParameterSource()
                        .addValue("teacherId", teacherId)
                        .addValue("timeslotId", timeslotId));
                if (!unavailList.isEmpty()) {
                     String reason = (String) unavailList.get(0).get("reason");
                     String reasonStr = (reason != null && !reason.isBlank()) ? " (" + reason + ")" : "";
                     conflicts.put("teacherUnavailable", "Giáo viên đã đăng ký lịch bận cố định ca học này" + reasonStr);
                }
            }
 
            // Check trùng phòng học trong DB (ngoại trừ các lessonId thuộc batch)
            if (roomId != null && roomId > 0) {
                String roomSql = """
                        SELECT c.name AS class_name, l.lesson_index
                        FROM lesson_assignment la
                        INNER JOIN lesson l ON la.lesson_id = l.id
                        INNER JOIN class c ON l.class_id = c.id
                        WHERE la.room_id = :roomId
                          AND la.timeslot_id = :timeslotId
                          AND la.session_date = :sessionDate
                          AND la.lesson_id NOT IN (:lessonIds)
                          AND la.is_deleted = 0 AND l.is_deleted = 0 AND c.is_deleted = 0
                        LIMIT 1
                        """;
                var roomParams = new MapSqlParameterSource()
                        .addValue("roomId", roomId)
                        .addValue("timeslotId", timeslotId)
                        .addValue("sessionDate", date)
                        .addValue("lessonIds", request.getLessonIds());
                List<java.util.Map<String, Object>> roomConflicts = jdbc.queryForList(roomSql, roomParams);
                if (!roomConflicts.isEmpty()) {
                     String className = (String) roomConflicts.get(0).get("class_name");
                     int index = ((Number) roomConflicts.get(0).get("lesson_index")).intValue();
                     conflicts.put("roomConflict", String.format("Phòng học đã bị chiếm bởi lớp %s (Buổi #%d) tại ca này.", className, index));
                }
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success(conflicts));
    }
}

