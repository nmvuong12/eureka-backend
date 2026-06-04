package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.ClassStatus;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.SchoolClass;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý lớp học và buổi học.
 * Sử dụng NamedParameterJdbcTemplate - KHÔNG dùng JPA/Hibernate.
 * Giữ tương thích ngược với các method cũ đồng thời bổ sung tính năng Rolling Scheduling.
 */
@Repository
@RequiredArgsConstructor
public class ClassRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * RowMapper ánh xạ ResultSet → SchoolClass entity.
     * Bao gồm cả legacy fields và các field mới cho Rolling Scheduling.
     */
    private final RowMapper<SchoolClass> classMapper = (rs, row) -> {
        // Parse ClassStatus (hỗ trợ cả old enum values và new)
        ClassStatus classStatus = null;
        try {
            String st = rs.getString("status");
            if (st != null) classStatus = ClassStatus.valueOf(st);
        } catch (IllegalArgumentException ignored) {}

        return SchoolClass.builder()
                .id(rs.getLong("id"))
                // Legacy fields
                .courseId(rs.getObject("course_id") != null ? rs.getLong("course_id") : null)
                .name(rs.getString("name"))
                .studentSize(rs.getObject("student_size") != null ? rs.getInt("student_size") : null)
                .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
                // New fields
                .classCode(rs.getString("class_code"))
                .batchId(rs.getObject("batch_id") != null ? rs.getLong("batch_id") : null)
                .schedulePatternId(rs.getObject("schedule_pattern_id") != null ? rs.getLong("schedule_pattern_id") : null)
                .studentCount(rs.getObject("student_count") != null ? rs.getInt("student_count") : 0)
                .status(classStatus)
                .actualOpeningDate(rs.getDate("actual_opening_date") != null
                        ? rs.getDate("actual_opening_date").toLocalDate() : null)
                .note(tryGetString(rs, "note"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                // Join fields (có thể null nếu query không JOIN)
                .batchName(tryGetString(rs, "batch_name"))
                .courseName(tryGetString(rs, "course_name"))
                .courseCode(tryGetString(rs, "course_code"))
                .patternCode(tryGetString(rs, "pattern_code"))
                .patternLabel(tryGetString(rs, "pattern_label"))
                .teacherName(tryGetString(rs, "teacher_name"))
                .build();
    };

    /** Helper: lấy String từ ResultSet, trả null nếu column không tồn tại */
    private String tryGetString(java.sql.ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Helper: lấy Long từ ResultSet, trả null nếu column không tồn tại hoặc NULL */
    private Long tryGetLong(java.sql.ResultSet rs, String col) {
        try {
            long val = rs.getLong(col);
            return rs.wasNull() ? null : val;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Helper: lấy LocalDate từ ResultSet, trả null nếu column không tồn tại hoặc NULL */
    private java.time.LocalDate tryGetLocalDate(java.sql.ResultSet rs, String col) {
        try {
            java.sql.Date d = rs.getDate(col);
            return d != null ? d.toLocalDate() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private final RowMapper<Lesson> lessonMapper = (rs, row) -> Lesson.builder()
            .id(rs.getLong("id"))
            .classId(rs.getLong("class_id"))
            .lessonIndex(rs.getInt("lesson_index"))
            .requiredSkill(rs.getString("required_skill"))
            .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
            .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
            .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
            .pinned(rs.getBoolean("is_pinned"))
            .schedulePatternId(tryGetLong(rs, "schedule_pattern_id"))
            .classTeacherId(tryGetLong(rs, "class_teacher_id"))
            .sessionDate(tryGetLocalDate(rs, "session_date"))
            .originalSessionDate(tryGetLocalDate(rs, "original_session_date"))
            .originalTimeslotId(tryGetLong(rs, "original_timeslot_id"))
            .originalRoomId(tryGetLong(rs, "original_room_id"))
            .rescheduleReason(tryGetString(rs, "reschedule_reason"))
            .leaveRequestId(tryGetLong(rs, "leave_request_id"))
            .build();

    // ==================== LEGACY METHODS (giữ nguyên tương thích ngược) ====================

    /** Lấy tất cả lớp học chưa xóa (legacy - không JOIN) */
    public List<SchoolClass> findAll() {
        String sql = """
                SELECT c.id, c.course_id, c.name, c.student_size, c.start_date, c.status,
                       c.class_code, c.batch_id, c.schedule_pattern_id, c.student_count,
                       c.actual_opening_date, c.note, c.created_at, c.updated_at
                FROM class c
                WHERE c.is_deleted = 0
                ORDER BY c.start_date DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), classMapper);
    }

    /** Tìm lớp theo ID (legacy) */
    public Optional<SchoolClass> findById(Long id) {
        String sql = """
                SELECT id, course_id, name, student_size, start_date, status,
                       class_code, batch_id, schedule_pattern_id, student_count,
                       actual_opening_date, note, created_at, updated_at
                FROM class WHERE id = :id AND is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), classMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Tạo lớp học mới (legacy - tương thích với ClassService cũ) */
    public Long saveClass(SchoolClass c) {
        String sql = """
                INSERT INTO class (course_id, name, student_size, start_date, status)
                VALUES (:courseId, :name, :studentSize, :startDate, :status)
                """;
        var params = new MapSqlParameterSource()
                .addValue("courseId", c.getCourseId())
                .addValue("name", c.getName())
                .addValue("studentSize", c.getStudentSize())
                .addValue("startDate", c.getStartDate())
                .addValue("status", c.getStatus() != null ? c.getStatus().name() : "PENDING");
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    /** Lưu hàng loạt buổi học */
    public void saveLessons(List<Lesson> lessons) {
        String sql = """
                INSERT INTO lesson (class_id, lesson_index, required_skill)
                VALUES (:classId, :lessonIndex, :requiredSkill)
                """;
        var batchParams = lessons.stream()
                .map(l -> new MapSqlParameterSource()
                        .addValue("classId", l.getClassId())
                        .addValue("lessonIndex", l.getLessonIndex())
                        .addValue("requiredSkill", l.getRequiredSkill()))
                .toArray(MapSqlParameterSource[]::new);
        jdbc.batchUpdate(sql, batchParams);

        // Tạo lesson_assignment rỗng cho mỗi lesson
        String assignSql = """
                INSERT INTO lesson_assignment (lesson_id, is_pinned)
                SELECT l.id, 0 FROM lesson l WHERE l.class_id = :classId
                ON DUPLICATE KEY UPDATE lesson_id = lesson_id
                """;
        jdbc.update(assignSql, new MapSqlParameterSource("classId", lessons.get(0).getClassId()));
    }

    /** Lấy danh sách buổi học theo lớp */
    public List<Lesson> findLessonsByClassId(Long classId) {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       COALESCE(la.teacher_id, NULL) AS teacher_id,
                       COALESCE(la.room_id, NULL) AS room_id,
                       COALESCE(la.timeslot_id, NULL) AS timeslot_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned,
                       la.session_date,
                       la.original_session_date,
                       la.original_timeslot_id,
                       la.original_room_id,
                       la.reschedule_reason,
                       la.leave_request_id,
                       c.schedule_pattern_id,
                       c.teacher_id AS class_teacher_id
                FROM lesson l
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                LEFT JOIN class c ON l.class_id = c.id
                WHERE l.class_id = :classId AND l.is_deleted = 0
                ORDER BY l.lesson_index
                """;
        return jdbc.query(sql, new MapSqlParameterSource("classId", classId), lessonMapper);
    }

    /**
     * Lấy tất cả buổi học cho Solver (chỉ lớp OPEN, STUDYING).
     * CHỈ LẤY CÁC BUỔI ĐẠI DIỆN TRONG TUẦN (lesson_index <= sessions_per_week)
     */
    public List<Lesson> findAllLessonsForSolver() {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id,
                       la.session_date,
                       la.original_session_date,
                       la.original_timeslot_id,
                       la.original_room_id,
                       la.reschedule_reason,
                       la.leave_request_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned,
                       c.schedule_pattern_id,
                       c.teacher_id AS class_teacher_id
                FROM lesson l
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                LEFT JOIN class c ON l.class_id = c.id
                INNER JOIN schedule_pattern sp ON c.schedule_pattern_id = sp.id
                WHERE c.status IN ('OPEN', 'STUDYING') 
                  AND c.is_deleted = 0 
                  AND l.is_deleted = 0
                  AND l.lesson_index <= sp.sessions_per_week
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), lessonMapper);
    }

    /** Cập nhật assignment cho một buổi học */
    public int updateAssignment(Long lessonId, Long teacherId, Long roomId, Long timeslotId) {
        String sql = """
                INSERT INTO lesson_assignment (lesson_id, teacher_id, room_id, timeslot_id)
                VALUES (:lessonId, :teacherId, :roomId, :timeslotId)
                ON DUPLICATE KEY UPDATE
                  teacher_id = VALUES(teacher_id),
                  room_id = VALUES(room_id),
                  timeslot_id = VALUES(timeslot_id),
                  updated_at = NOW()
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("lessonId", lessonId)
                .addValue("teacherId", teacherId)
                .addValue("roomId", roomId)
                .addValue("timeslotId", timeslotId));
    }

    /** Cập nhật hàng loạt assignment (không ghi đè pinned) */
    public void batchUpdateAssignments(List<Lesson> lessons) {
        String sql = """
                INSERT INTO lesson_assignment (lesson_id, teacher_id, room_id, timeslot_id)
                VALUES (:lessonId, :teacherId, :roomId, :timeslotId)
                ON DUPLICATE KEY UPDATE
                  teacher_id = VALUES(teacher_id),
                  room_id = VALUES(room_id),
                  timeslot_id = VALUES(timeslot_id),
                  updated_at = NOW()
                """;
        var batchParams = lessons.stream()
                .filter(l -> !Boolean.TRUE.equals(l.getPinned()))
                .map(l -> new MapSqlParameterSource()
                        .addValue("lessonId", l.getId())
                        .addValue("teacherId", l.getTeacherId())
                        .addValue("roomId", l.getRoomId())
                        .addValue("timeslotId", l.getTimeslotId()))
                .toArray(MapSqlParameterSource[]::new);
        if (batchParams.length > 0) jdbc.batchUpdate(sql, batchParams);
    }

    /** Xóa assignment của giáo viên trong khoảng thời gian (không ảnh hưởng pinned) */
    public int clearAssignmentsForTeacherInDateRange(Long teacherId, java.time.LocalDate from, java.time.LocalDate to) {
        String sql = """
                UPDATE lesson_assignment SET teacher_id = NULL, updated_at = NOW()
                WHERE teacher_id = :teacherId AND is_pinned = 0
                """;
        return jdbc.update(sql, new MapSqlParameterSource("teacherId", teacherId));
    }

    /** Lấy danh sách buổi học theo giáo viên phụ trách */
    public List<Lesson> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id,
                       la.session_date,
                       la.original_session_date,
                       la.original_timeslot_id,
                       la.original_room_id,
                       la.reschedule_reason,
                       la.leave_request_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned,
                       c.schedule_pattern_id,
                       c.teacher_id AS class_teacher_id
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id AND la.teacher_id = :teacherId
                LEFT JOIN class c ON l.class_id = c.id
                WHERE c.is_deleted = 0 AND l.is_deleted = 0
                ORDER BY la.timeslot_id, l.class_id, l.lesson_index
                """;
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), lessonMapper);
    }

    /** Xóa mềm lớp học và toàn bộ buổi học liên quan */
    public void deleteClass(Long id) {
        jdbc.update("UPDATE class SET is_deleted = 1 WHERE id = :id", new MapSqlParameterSource("id", id));
        jdbc.update("UPDATE lesson SET is_deleted = 1 WHERE class_id = :id", new MapSqlParameterSource("id", id));
    }

    // ==================== NEW METHODS (Rolling Scheduling) ====================

    /**
     * Lấy danh sách lớp theo batch ID, có JOIN đầy đủ thông tin
     * @param batchId ID kế hoạch khai giảng
     */
    public List<SchoolClass> findByBatchId(Long batchId) {
        String sql = """
                SELECT c.id, c.course_id, c.name, c.student_size, c.start_date, c.status,
                       c.class_code, c.batch_id, c.schedule_pattern_id, c.student_count,
                       c.actual_opening_date, c.note, c.created_at, c.updated_at,
                       cb.batch_name, co.name AS course_name, co.code AS course_code,
                       sp.code AS pattern_code,
                       CONCAT(sp.study_days, ' ', sp.slot_code, ' ', sp.slot_start, '-', sp.slot_end) AS pattern_label,
                       t.full_name AS teacher_name
                FROM class c
                LEFT JOIN course_batch cb ON c.batch_id = cb.id
                LEFT JOIN course co ON cb.course_id = co.id
                LEFT JOIN schedule_pattern sp ON c.schedule_pattern_id = sp.id
                LEFT JOIN teacher t ON c.teacher_id = t.id
                WHERE c.batch_id = :batchId AND c.is_deleted = 0
                ORDER BY c.class_code
                """;
        return jdbc.query(sql, new MapSqlParameterSource("batchId", batchId), classMapper);
    }

    /**
     * Lọc lớp học theo nhiều tiêu chí đồng thời (tất cả optional)
     * @param batchId   filter theo kế hoạch khai giảng
     * @param status    filter theo trạng thái
     * @param patternId filter theo schedule pattern
     * @param teacherId filter theo giáo viên
     */
    public List<SchoolClass> findByFilters(Long batchId, String status, Long patternId, Long teacherId) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.course_id, c.name, c.student_size, c.start_date, c.status,
                       c.class_code, c.batch_id, c.schedule_pattern_id, c.student_count,
                       c.actual_opening_date, c.note, c.created_at, c.updated_at,
                       cb.batch_name, co.name AS course_name, co.code AS course_code,
                       sp.code AS pattern_code,
                       CONCAT(sp.study_days, ' ', sp.slot_code, ' ', sp.slot_start, '-', sp.slot_end) AS pattern_label,
                       t.full_name AS teacher_name
                FROM class c
                LEFT JOIN course_batch cb ON c.batch_id = cb.id
                LEFT JOIN course co ON cb.course_id = co.id
                LEFT JOIN schedule_pattern sp ON c.schedule_pattern_id = sp.id
                LEFT JOIN teacher t ON c.teacher_id = t.id
                WHERE c.is_deleted = 0
                """);
        var params = new MapSqlParameterSource();
        if (batchId != null) {
            sql.append(" AND c.batch_id = :batchId");
            params.addValue("batchId", batchId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status = :status");
            params.addValue("status", status);
        }
        if (patternId != null) {
            sql.append(" AND c.schedule_pattern_id = :patternId");
            params.addValue("patternId", patternId);
        }
        if (teacherId != null) {
            sql.append(" AND c.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        sql.append(" ORDER BY c.class_code");
        return jdbc.query(sql.toString(), params, classMapper);
    }

    /**
     * Đếm số lớp đang dùng một pattern (trạng thái khác CANCELLED và FINISHED).
     * Dùng để tính capacity còn lại (áp dụng cơ chế giữ chỗ trước).
     */
    public int countByPatternAndActiveStatus(Long patternId) {
        String sql = """
                SELECT COUNT(*) FROM class
                WHERE schedule_pattern_id = :patternId
                  AND status NOT IN ('CANCELLED', 'FINISHED')
                  AND is_deleted = 0
                """;
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("patternId", patternId), Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Lấy danh sách teacher_id đang bị bận trong pattern này (trạng thái khác CANCELLED và FINISHED).
     * Dùng để tính số GV còn rảnh.
     */
    public List<Long> findAssignedTeacherIdsByPattern(Long patternId) {
        String sql = """
                SELECT DISTINCT teacher_id FROM class
                WHERE schedule_pattern_id = :patternId
                  AND status NOT IN ('CANCELLED', 'FINISHED')
                  AND teacher_id IS NOT NULL
                  AND is_deleted = 0
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("patternId", patternId), Long.class);
    }

    /**
     * Lấy danh sách room_id đang bị bận trong pattern này (trạng thái khác CANCELLED và FINISHED).
     * Dùng để tính số phòng còn rảnh.
     */
    public List<Long> findAssignedRoomIdsByPattern(Long patternId) {
        String sql = """
                SELECT DISTINCT room_id FROM class
                WHERE schedule_pattern_id = :patternId
                  AND status NOT IN ('CANCELLED', 'FINISHED')
                  AND room_id IS NOT NULL
                  AND is_deleted = 0
                """;
        return jdbc.queryForList(sql, new MapSqlParameterSource("patternId", patternId), Long.class);
    }

    /**
     * Lưu lớp học mới theo quy trình Rolling Scheduling (có class_code, batch_id, pattern_id).
     */
    public Long savePlanningClass(SchoolClass c) {
        String sql = """
                INSERT INTO class (class_code, batch_id, schedule_pattern_id, course_id,
                                   name, student_size, student_count, start_date, actual_opening_date, status, note)
                VALUES (:classCode, :batchId, :patternId, :courseId,
                        :name, :studentSize, :studentCount, :startDate, :actualOpeningDate, :status, :note)
                """;
        var params = new MapSqlParameterSource()
                .addValue("classCode", c.getClassCode())
                .addValue("batchId", c.getBatchId())
                .addValue("patternId", c.getSchedulePatternId())
                .addValue("courseId", c.getCourseId())
                .addValue("name", c.getClassCode()) // name = classCode
                .addValue("studentSize", c.getStudentSize() != null ? c.getStudentSize() : (c.getStudentCount() != null ? c.getStudentCount() : 0))
                .addValue("studentCount", c.getStudentCount() != null ? c.getStudentCount() : 0)
                .addValue("startDate", c.getStartDate() != null ? c.getStartDate() : (c.getActualOpeningDate() != null ? c.getActualOpeningDate() : java.time.LocalDate.now()))
                .addValue("actualOpeningDate", c.getActualOpeningDate())
                .addValue("status", c.getStatus() != null ? c.getStatus().name() : "DRAFT")
                .addValue("note", c.getNote());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    /** Cập nhật thông tin chi tiết lớp học (tên, sĩ số tối đa, giáo viên phụ trách, ghi chú) */
    public int updateClassDetails(SchoolClass c) {
        String sql = """
                UPDATE class SET
                  name = :name,
                  class_code = :classCode,
                  student_size = :studentSize,
                  teacher_id = :teacherId,
                  note = :note,
                  updated_at = NOW()
                WHERE id = :id AND is_deleted = 0
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", c.getId())
                .addValue("name", c.getName())
                .addValue("classCode", c.getClassCode())
                .addValue("studentSize", c.getStudentSize())
                .addValue("teacherId", c.getTeacherId())
                .addValue("note", c.getNote()));
    }

    /** Cập nhật trạng thái lớp học */
    public int updateStatus(Long id, String status) {
        return jdbc.update(
                "UPDATE class SET status=:status, updated_at=NOW() WHERE id=:id AND is_deleted=0",
                new MapSqlParameterSource("status", status).addValue("id", id));
    }

    /** Cập nhật sĩ số thực tế của lớp */
    public int updateStudentCount(Long id, int count) {
        return jdbc.update(
                "UPDATE class SET student_count=:count, student_size=:count, updated_at=NOW() WHERE id=:id AND is_deleted=0",
                new MapSqlParameterSource("count", count).addValue("id", id));
    }

    /** Gán giáo viên và phòng học cho lớp (sau khi solver chạy) */
    public int updateTeacherAndRoom(Long id, Long teacherId, Long roomId) {
        return jdbc.update(
                "UPDATE class SET teacher_id=:teacherId, room_id=:roomId, updated_at=NOW() WHERE id=:id AND is_deleted=0",
                new MapSqlParameterSource("teacherId", teacherId).addValue("roomId", roomId).addValue("id", id));
    }

    /**
     * Tìm suffix lớn nhất hiện có cho batch và prefix nhất định.
     * VD: prefix="IELTS_", đã có IELTS_A, IELTS_B → maxSuffix = 1 (B = index 1)
     * Dùng để tiếp tục sinh classCode theo thứ tự alphabet.
     */
    public int findMaxSuffixForBatch(Long batchId, String codePrefix) {
        String sql = """
                SELECT COUNT(*) FROM class
                WHERE batch_id = :batchId AND class_code LIKE :prefix AND is_deleted = 0
                """;
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("batchId", batchId).addValue("prefix", codePrefix + "%"),
                Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Sao lưu lịch học gốc và chuyển đổi lịch dạy bù cho buổi học (Reschedule)
     */
    public int backupAndReschedule(Long lessonId, java.time.LocalDate newDate, Long newTimeslotId, Long newRoomId, String reason, Long leaveRequestId) {
        String sql = """
                UPDATE lesson_assignment
                SET 
                  original_session_date = COALESCE(original_session_date, session_date),
                  original_timeslot_id = COALESCE(original_timeslot_id, timeslot_id),
                  original_room_id = COALESCE(original_room_id, room_id),
                  session_date = :newDate,
                  timeslot_id = :newTimeslotId,
                  room_id = :newRoomId,
                  reschedule_reason = :reason,
                  leave_request_id = :leaveRequestId,
                  is_pinned = 1,
                  updated_at = NOW()
                WHERE lesson_id = :lessonId
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("lessonId", lessonId)
                .addValue("newDate", newDate)
                .addValue("newTimeslotId", newTimeslotId)
                .addValue("newRoomId", newRoomId)
                .addValue("reason", reason)
                .addValue("leaveRequestId", leaveRequestId));
    }

    /**
     * Sao lưu lịch học gốc và xoá giáo viên phụ trách để chuẩn bị tìm người dạy thay
     */
    public int backupAndRemoveTeacher(Long lessonId, String reason, Long leaveRequestId) {
        String sql = """
                UPDATE lesson_assignment
                SET 
                  original_session_date = COALESCE(original_session_date, session_date),
                  original_timeslot_id = COALESCE(original_timeslot_id, timeslot_id),
                  original_room_id = COALESCE(original_room_id, room_id),
                  teacher_id = NULL,
                  reschedule_reason = :reason,
                  leave_request_id = :leaveRequestId,
                  is_pinned = 1,
                  updated_at = NOW()
                WHERE lesson_id = :lessonId
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("lessonId", lessonId)
                .addValue("reason", reason)
                .addValue("leaveRequestId", leaveRequestId));
    }
}
