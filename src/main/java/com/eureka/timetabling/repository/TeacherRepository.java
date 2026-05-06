package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.solver.Timetable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu giáo viên bằng raw SQL (không dùng JPA relationship)
 */
@Repository
@RequiredArgsConstructor
public class TeacherRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<Teacher> teacherMapper = (rs, row) -> Teacher.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .email(rs.getString("email"))
            .phone(rs.getString("phone"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null
                    ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Teacher> findAll(String status) {
        String sql = """
                SELECT id, name, email, phone, status, created_at, updated_at
                FROM teacher
                WHERE (:status IS NULL OR status = :status)
                ORDER BY name
                """;
        return jdbc.query(sql, new MapSqlParameterSource("status", status), teacherMapper);
    }

    public Optional<Teacher> findById(Long id) {
        String sql = """
                SELECT id, name, email, phone, status, created_at, updated_at
                FROM teacher
                WHERE id = :id
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), teacherMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(Teacher teacher) {
        String sql = """
                INSERT INTO teacher (name, email, phone, status)
                VALUES (:name, :email, :phone, :status)
                """;
        var params = new MapSqlParameterSource()
                .addValue("name", teacher.getName())
                .addValue("email", teacher.getEmail())
                .addValue("phone", teacher.getPhone())
                .addValue("status", teacher.getStatus() != null ? teacher.getStatus() : "ACTIVE");
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public int update(Teacher teacher) {
        String sql = """
                UPDATE teacher
                SET name = :name, email = :email, phone = :phone, status = :status, updated_at = NOW()
                WHERE id = :id
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", teacher.getId())
                .addValue("name", teacher.getName())
                .addValue("email", teacher.getEmail())
                .addValue("phone", teacher.getPhone())
                .addValue("status", teacher.getStatus());
        return jdbc.update(sql, params);
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM teacher WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE email = :email";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByEmailAndIdNot(String email, Long id) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE email = :email AND id != :id";
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("email", email).addValue("id", id), Integer.class);
        return count != null && count > 0;
    }

    // Truy vấn kỹ năng
    public List<String> findSkillsByTeacherId(Long teacherId) {
        String sql = "SELECT skill_code FROM teacher_skill WHERE teacher_id = :teacherId ORDER BY skill_code";
        return jdbc.queryForList(sql, new MapSqlParameterSource("teacherId", teacherId), String.class);
    }

    public void replaceSkills(Long teacherId, List<String> skills) {
        jdbc.update("DELETE FROM teacher_skill WHERE teacher_id = :teacherId",
                new MapSqlParameterSource("teacherId", teacherId));
        if (skills != null && !skills.isEmpty()) {
            String insertSql = "INSERT IGNORE INTO teacher_skill (teacher_id, skill_code) VALUES (:teacherId, :skillCode)";
            var batchParams = skills.stream()
                    .map(s -> new MapSqlParameterSource("teacherId", teacherId).addValue("skillCode", s))
                    .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate(insertSql, batchParams);
        }
    }

    // Truy vấn tập hợp kỹ năng cho Timefold
    public List<Timetable.TeacherSkillFact> findAllSkillFacts() {
        String sql = "SELECT teacher_id, skill_code FROM teacher_skill";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.TeacherSkillFact(rs.getLong("teacher_id"), rs.getString("skill_code")));
    }

    // Tìm ID tất cả giáo viên đang hoạt động
    public List<Long> findAllActiveIds() {
        return jdbc.queryForList(
                "SELECT id FROM teacher WHERE status = 'ACTIVE'",
                new MapSqlParameterSource(), Long.class);
    }

    // Tìm giáo viên thay thế phù hợp kỹ năng và không bận ca đó
    public List<Teacher> findSuitableSubstitutes(String skillCode, Long timeslotId, Long excludeTeacherId) {
        String sql = """
                SELECT DISTINCT t.id, t.name, t.email, t.phone, t.status, t.created_at, t.updated_at
                FROM teacher t
                INNER JOIN teacher_skill ts ON t.id = ts.teacher_id AND ts.skill_code = :skillCode
                LEFT JOIN teacher_unavailable tu ON t.id = tu.teacher_id AND tu.timeslot_id = :timeslotId
                LEFT JOIN lesson_assignment la ON t.id = la.teacher_id AND la.timeslot_id = :timeslotId
                WHERE t.status = 'ACTIVE'
                  AND t.id != :excludeTeacherId
                  AND tu.id IS NULL
                  AND la.id IS NULL
                ORDER BY t.name
                """;
        var params = new MapSqlParameterSource()
                .addValue("skillCode", skillCode)
                .addValue("timeslotId", timeslotId)
                .addValue("excludeTeacherId", excludeTeacherId);
        return jdbc.query(sql, params, teacherMapper);
    }
}
