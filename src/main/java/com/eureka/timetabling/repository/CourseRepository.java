package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Course;
import com.eureka.timetabling.domain.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý khóa học (Course Catalog).
 * Sử dụng NamedParameterJdbcTemplate - KHÔNG dùng JPA/Hibernate.
 */
@Repository
@RequiredArgsConstructor
public class CourseRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** RowMapper ánh xạ ResultSet → Course entity, bao gồm cả legacy fields */
    private final RowMapper<Course> courseMapper = (rs, row) -> {
        String statusStr = rs.getString("status");
        CourseStatus status = null;
        try {
            if (statusStr != null) status = CourseStatus.valueOf(statusStr);
        } catch (IllegalArgumentException ignored) {}

        return Course.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .totalSessions(rs.getObject("total_lessons") != null ? rs.getInt("total_lessons") : null)
                .totalLessons(rs.getObject("total_lessons") != null ? rs.getInt("total_lessons") : null)
                .sessionsPerWeek(rs.getObject("sessions_per_week") != null ? rs.getInt("sessions_per_week") : null)
                .durationWeeks(rs.getObject("duration_weeks") != null ? rs.getInt("duration_weeks") : null)
                .minStudents(rs.getObject("min_students") != null ? rs.getInt("min_students") : null)
                .maxStudents(rs.getObject("max_students") != null ? rs.getInt("max_students") : null)
                .requiredSkillCode(rs.getString("required_skill_code"))
                .status(status)
                .defaultDuration(rs.getObject("default_duration") != null ? rs.getInt("default_duration") : null)
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();
    };

    /** Lấy tất cả khóa học chưa xóa, sắp xếp theo tên */
    public List<Course> findAll() {
        String sql = """
                SELECT id, code, name, description, total_lessons, sessions_per_week, duration_weeks,
                       min_students, max_students, required_skill_code, status,
                       default_duration, created_at, updated_at
                FROM course
                WHERE is_deleted = 0
                ORDER BY name
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), courseMapper);
    }

    /** Tìm khóa học theo ID */
    public Optional<Course> findById(Long id) {
        String sql = """
                SELECT id, code, name, description, total_lessons, sessions_per_week, duration_weeks,
                       min_students, max_students, required_skill_code, status,
                       default_duration, created_at, updated_at
                FROM course WHERE id = :id AND is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), courseMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Tìm khóa học theo mã code (unique) */
    public Optional<Course> findByCode(String code) {
        String sql = """
                SELECT id, code, name, description, total_lessons, sessions_per_week, duration_weeks,
                       min_students, max_students, required_skill_code, status,
                       default_duration, created_at, updated_at
                FROM course WHERE code = :code AND is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("code", code), courseMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Tìm kiếm có phân trang - hỗ trợ tìm theo name/code và filter theo status
     * @param query   từ khóa tìm kiếm (nullable)
     * @param status  trạng thái filter (nullable)
     * @param page    trang bắt đầu từ 1
     * @param size    kích thước trang
     */
    public List<Course> searchPaged(String query, String status, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, code, name, description, total_lessons, sessions_per_week, duration_weeks,
                       min_students, max_students, required_skill_code, status,
                       default_duration, created_at, updated_at
                FROM course WHERE is_deleted = 0
                """);
        var params = new MapSqlParameterSource();
        buildSearchCondition(sql, params, query, status);
        sql.append(" ORDER BY name LIMIT :limit OFFSET :offset");
        params.addValue("limit", size).addValue("offset", (page - 1) * size);
        return jdbc.query(sql.toString(), params, courseMapper);
    }

    /** Đếm tổng số bản ghi khớp điều kiện tìm kiếm (phục vụ phân trang) */
    public long countSearch(String query, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM course WHERE is_deleted = 0");
        var params = new MapSqlParameterSource();
        buildSearchCondition(sql, params, query, status);
        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    /** Helper: thêm điều kiện tìm kiếm vào query builder */
    private void buildSearchCondition(StringBuilder sql, MapSqlParameterSource params,
                                      String query, String status) {
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE LOWER(:query) OR LOWER(code) LIKE LOWER(:query))");
            params.addValue("query", "%" + query.trim() + "%");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status);
        }
    }

    /** Lưu khóa học mới, trả về ID được sinh tự động */
    public Long save(Course course) {
        String sql = """
                INSERT INTO course (code, name, description, total_lessons, sessions_per_week,
                                    duration_weeks, min_students, max_students,
                                    required_skill_code, status, default_duration)
                VALUES (:code, :name, :desc, :total, :spw, :dw, :minS, :maxS, :skill, :status, :duration)
                """;
        var params = buildSaveParams(course);
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    /** Cập nhật toàn bộ thông tin khóa học */
    public int update(Course course) {
        String sql = """
                UPDATE course SET code=:code, name=:name, description=:desc, total_lessons=:total,
                    sessions_per_week=:spw, duration_weeks=:dw, min_students=:minS, max_students=:maxS,
                    required_skill_code=:skill, status=:status,
                    default_duration=:duration, updated_at=NOW()
                WHERE id=:id AND is_deleted=0
                """;
        var params = buildSaveParams(course);
        params.addValue("id", course.getId());
        return jdbc.update(sql, params);
    }

    /** Thay đổi trạng thái khóa học */
    public int changeStatus(Long id, String status) {
        return jdbc.update(
                "UPDATE course SET status=:status, updated_at=NOW() WHERE id=:id AND is_deleted=0",
                new MapSqlParameterSource("status", status).addValue("id", id));
    }

    /** Xóa mềm khóa học (is_deleted = 1) */
    public int deleteById(Long id) {
        return jdbc.update("UPDATE course SET is_deleted=1, updated_at=NOW() WHERE id=:id",
                new MapSqlParameterSource("id", id));
    }

    /** Helper: tạo MapSqlParameterSource dùng chung cho save/update */
    private MapSqlParameterSource buildSaveParams(Course c) {
        return new MapSqlParameterSource()
                .addValue("code", c.getCode())
                .addValue("name", c.getName())
                .addValue("desc", c.getDescription())
                .addValue("total", c.getTotalSessions() != null ? c.getTotalSessions() : c.getTotalLessons())
                .addValue("spw", c.getSessionsPerWeek())
                .addValue("dw", c.getDurationWeeks())
                .addValue("minS", c.getMinStudents())
                .addValue("maxS", c.getMaxStudents())
                .addValue("skill", c.getRequiredSkillCode())
                .addValue("status", c.getStatus() != null ? c.getStatus().name() : "ACTIVE")
                .addValue("duration", c.getDefaultDuration());
    }
}
