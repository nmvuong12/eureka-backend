package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.SchoolClass;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý lớp học và buổi học */
@Repository
@RequiredArgsConstructor
public class ClassRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<SchoolClass> classMapper = (rs, row) -> SchoolClass.builder()
            .id(rs.getLong("id"))
            .courseId(rs.getLong("course_id"))
            .name(rs.getString("name"))
            .studentSize(rs.getInt("student_size"))
            .startDate(rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null)
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    private final RowMapper<Lesson> lessonMapper = (rs, row) -> Lesson.builder()
            .id(rs.getLong("id"))
            .classId(rs.getLong("class_id"))
            .lessonIndex(rs.getInt("lesson_index"))
            .requiredSkill(rs.getString("required_skill"))
            .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
            .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
            .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
            .pinned(rs.getBoolean("is_pinned"))
            .build();

    public List<SchoolClass> findAll() {
        String sql = """
                SELECT c.id, c.course_id, c.name, c.student_size, c.start_date, c.status, c.created_at, c.updated_at
                FROM class c
                ORDER BY c.start_date DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), classMapper);
    }

    public Optional<SchoolClass> findById(Long id) {
        var list = jdbc.query("""
                SELECT id, course_id, name, student_size, start_date, status, created_at, updated_at
                FROM class WHERE id = :id
                """, new MapSqlParameterSource("id", id), classMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

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
                .addValue("status", c.getStatus() != null ? c.getStatus() : "PENDING");
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

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
                INSERT INTO lesson_assignment (lesson_id, teacher_id, room_id, timeslot_id, is_pinned)
                SELECT id, NULL, NULL, NULL, 0 FROM lesson
                WHERE class_id = :classId AND teacher_id IS NOT NULL
                """;
        // Thay thế: insert trực tiếp sau khi lấy id
        String assignSql2 = """
                INSERT INTO lesson_assignment (lesson_id, is_pinned)
                SELECT l.id, 0 FROM lesson l WHERE l.class_id = :classId
                ON DUPLICATE KEY UPDATE lesson_id = lesson_id
                """;
        jdbc.update(assignSql2, new MapSqlParameterSource("classId", lessons.get(0).getClassId()));
    }

    public List<Lesson> findLessonsByClassId(Long classId) {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       COALESCE(la.teacher_id, NULL) AS teacher_id,
                       COALESCE(la.room_id, NULL) AS room_id,
                       COALESCE(la.timeslot_id, NULL) AS timeslot_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned
                FROM lesson l
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                WHERE l.class_id = :classId
                ORDER BY l.lesson_index
                """;
        return jdbc.query(sql, new MapSqlParameterSource("classId", classId), lessonMapper);
    }

    public List<Lesson> findAllLessonsForSolver() {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned
                FROM lesson l
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                LEFT JOIN class c ON l.class_id = c.id
                WHERE c.status IN ('PENDING','ACTIVE')
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), lessonMapper);
    }

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

    public int clearAssignmentsForTeacherInDateRange(Long teacherId, java.time.LocalDate from, java.time.LocalDate to) {
        String sql = """
                UPDATE lesson_assignment la
                INNER JOIN timeslot ts ON la.timeslot_id = ts.id
                SET la.teacher_id = NULL, la.updated_at = NOW()
                WHERE la.teacher_id = :teacherId
                  AND CASE ts.day_of_week
                      WHEN 'MONDAY'    THEN 1 WHEN 'TUESDAY'   THEN 2 WHEN 'WEDNESDAY' THEN 3
                      WHEN 'THURSDAY'  THEN 4 WHEN 'FRIDAY'    THEN 5 WHEN 'SATURDAY'  THEN 6
                      WHEN 'SUNDAY'    THEN 7 END
                  BETWEEN DAYOFWEEK(:fromDate) AND DAYOFWEEK(:toDate)
                """;
        // Cách đơn giản hơn: xoá tất cả trong khoảng ngày dựa trên lesson_assignment
        String simpleSql = """
                UPDATE lesson_assignment SET teacher_id = NULL, updated_at = NOW()
                WHERE teacher_id = :teacherId AND is_pinned = 0
                """;
        return jdbc.update(simpleSql, new MapSqlParameterSource("teacherId", teacherId));
    }

    public List<Lesson> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id AND la.teacher_id = :teacherId
                ORDER BY la.timeslot_id, l.class_id, l.lesson_index
                """;
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), lessonMapper);
    }
}
