package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.CourseBatch;
import com.eureka.timetabling.domain.CourseBatchStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý Kế hoạch khai giảng (CourseBatch).
 * Sử dụng NamedParameterJdbcTemplate - KHÔNG dùng JPA/Hibernate.
 */
@Repository
@RequiredArgsConstructor
public class CourseBatchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** RowMapper ánh xạ bản ghi kết hợp course_batch JOIN course */
    private final RowMapper<CourseBatch> batchMapper = (rs, row) -> {
        String statusStr = rs.getString("status");
        CourseBatchStatus status = null;
        try {
            if (statusStr != null) status = CourseBatchStatus.valueOf(statusStr);
        } catch (IllegalArgumentException ignored) {}

        return CourseBatch.builder()
                .id(rs.getLong("id"))
                .courseId(rs.getLong("course_id"))
                .batchName(rs.getString("batch_name"))
                .enrollmentStartDate(rs.getDate("enrollment_start_date") != null
                        ? rs.getDate("enrollment_start_date").toLocalDate() : null)
                .enrollmentEndDate(rs.getDate("enrollment_end_date") != null
                        ? rs.getDate("enrollment_end_date").toLocalDate() : null)
                .expectedOpeningDate(rs.getDate("expected_opening_date") != null
                        ? rs.getDate("expected_opening_date").toLocalDate() : null)
                .forecastScale(rs.getInt("forecast_scale"))
                .status(status)
                .note(rs.getString("note"))
                .courseName(rs.getString("course_name"))
                .courseCode(rs.getString("course_code"))
                .createdAt(rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null
                        ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();
    };

    /**
     * Lấy danh sách kế hoạch khai giảng với filter tuỳ chọn
     * @param courseId filter theo khóa học (nullable)
     * @param status   filter theo trạng thái (nullable)
     * @param expectedOpeningDate filter theo ngày khai giảng dự kiến (nullable)
     */
    public List<CourseBatch> findAll(Long courseId, String status, java.time.LocalDate expectedOpeningDate) {
        StringBuilder sql = new StringBuilder("""
                SELECT cb.id, cb.course_id, cb.batch_name, cb.enrollment_start_date,
                       cb.enrollment_end_date, cb.expected_opening_date, cb.forecast_scale,
                       cb.status, cb.note, cb.created_at, cb.updated_at,
                       c.name AS course_name, c.code AS course_code
                FROM course_batch cb
                LEFT JOIN course c ON cb.course_id = c.id
                WHERE 1=1
                """);
        var params = new MapSqlParameterSource();
        if (courseId != null) {
            sql.append(" AND cb.course_id = :courseId");
            params.addValue("courseId", courseId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND cb.status = :status");
            params.addValue("status", status);
        }
        if (expectedOpeningDate != null) {
            sql.append(" AND cb.expected_opening_date = :expectedOpeningDate");
            params.addValue("expectedOpeningDate", expectedOpeningDate);
        }
        sql.append(" ORDER BY cb.created_at DESC");
        return jdbc.query(sql.toString(), params, batchMapper);
    }

    /** Tìm kế hoạch theo ID, JOIN với course để lấy thông tin tên/mã khóa học */
    public Optional<CourseBatch> findById(Long id) {
        String sql = """
                SELECT cb.id, cb.course_id, cb.batch_name, cb.enrollment_start_date,
                       cb.enrollment_end_date, cb.expected_opening_date, cb.forecast_scale,
                       cb.status, cb.note, cb.created_at, cb.updated_at,
                       c.name AS course_name, c.code AS course_code
                FROM course_batch cb
                LEFT JOIN course c ON cb.course_id = c.id
                WHERE cb.id = :id
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), batchMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Lưu kế hoạch khai giảng mới, trả về ID được sinh tự động */
    public Long save(CourseBatch batch) {
        String sql = """
                INSERT INTO course_batch (course_id, batch_name, enrollment_start_date,
                    enrollment_end_date, expected_opening_date, forecast_scale, status, note)
                VALUES (:courseId, :batchName, :enrollStart, :enrollEnd, :expectedOpen, :scale, :status, :note)
                """;
        var params = buildParams(batch);
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    /** Cập nhật kế hoạch khai giảng */
    public int update(CourseBatch batch) {
        String sql = """
                UPDATE course_batch SET course_id=:courseId, batch_name=:batchName,
                    enrollment_start_date=:enrollStart, enrollment_end_date=:enrollEnd,
                    expected_opening_date=:expectedOpen, forecast_scale=:scale,
                    note=:note, updated_at=NOW()
                WHERE id=:id
                """;
        var params = buildParams(batch);
        params.addValue("id", batch.getId());
        return jdbc.update(sql, params);
    }

    /** Cập nhật trạng thái kế hoạch */
    public int updateStatus(Long id, String status) {
        return jdbc.update(
                "UPDATE course_batch SET status=:status, updated_at=NOW() WHERE id=:id",
                new MapSqlParameterSource("status", status).addValue("id", id));
    }

    /** Xóa kế hoạch khai giảng */
    public int delete(Long id) {
        return jdbc.update("DELETE FROM course_batch WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    /** Kiểm tra xem có batch nào của course đang ở trạng thái ENROLLING không */
    public boolean hasEnrollingBatchByCourse(Long courseId) {
        String sql = "SELECT COUNT(*) FROM course_batch WHERE course_id=:courseId AND status='ENROLLING'";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("courseId", courseId), Integer.class);
        return count != null && count > 0;
    }

    /** Helper: tạo params dùng chung cho save/update */
    private MapSqlParameterSource buildParams(CourseBatch b) {
        return new MapSqlParameterSource()
                .addValue("courseId", b.getCourseId())
                .addValue("batchName", b.getBatchName())
                .addValue("enrollStart", b.getEnrollmentStartDate())
                .addValue("enrollEnd", b.getEnrollmentEndDate())
                .addValue("expectedOpen", b.getExpectedOpeningDate())
                .addValue("scale", b.getForecastScale() != null ? b.getForecastScale() : 0)
                .addValue("status", b.getStatus() != null ? b.getStatus().name() : "PLANNING")
                .addValue("note", b.getNote());
    }
}
