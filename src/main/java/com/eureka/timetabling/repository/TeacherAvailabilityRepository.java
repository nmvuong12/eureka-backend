package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.TeacherAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

/**
 * Repository quản lý việc đăng ký lịch rảnh (TeacherAvailability) của giáo viên bán thời gian.
 * Sử dụng NamedParameterJdbcTemplate để thao tác trực tiếp với Database.
 */
@Repository
@RequiredArgsConstructor
public class TeacherAvailabilityRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * RowMapper ánh xạ từ ResultSet sang thực thể TeacherAvailability
     */
    private final RowMapper<TeacherAvailability> availabilityMapper = (rs, rowNum) -> TeacherAvailability.builder()
            .id(rs.getLong("id"))
            .teacherId(rs.getLong("teacher_id"))
            .dayOfWeek(rs.getString("day_of_week"))
            .startTime(rs.getTime("start_time").toLocalTime())
            .endTime(rs.getTime("end_time").toLocalTime())
            .build();

    /**
     * Lấy toàn bộ lịch rảnh đã đăng ký của một giáo viên
     */
    public List<TeacherAvailability> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT id, teacher_id, day_of_week, start_time, end_time
                FROM teacher_availability
                WHERE teacher_id = :teacherId
                ORDER BY FIELD(day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), start_time
                """;
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), availabilityMapper);
    }

    /**
     * Lưu lịch rảnh mới của giáo viên
     */
    public Long save(TeacherAvailability availability) {
        String sql = """
                INSERT INTO teacher_availability (teacher_id, day_of_week, start_time, end_time)
                VALUES (:teacherId, :dayOfWeek, :startTime, :endTime)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", availability.getTeacherId())
                .addValue("dayOfWeek", availability.getDayOfWeek())
                .addValue("startTime", java.sql.Time.valueOf(availability.getStartTime()))
                .addValue("endTime", java.sql.Time.valueOf(availability.getEndTime()));

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Kiểm tra xem lịch rảnh mới đăng ký có bị trùng lặp thời gian với các lịch đã có của giáo viên đó không.
     * Thuật toán kiểm tra giao thoa giữa hai khoảng thời gian [S1, E1] và [S2, E2]:
     * Trùng nhau khi và chỉ khi: S1 < E2 AND S2 < E1.
     */
    public boolean checkOverlap(Long teacherId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        String sql = """
                SELECT COUNT(*) FROM teacher_availability
                WHERE teacher_id = :teacherId
                  AND day_of_week = :dayOfWeek
                  AND start_time < :endTime
                  AND :startTime < end_time
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("dayOfWeek", dayOfWeek)
                .addValue("startTime", java.sql.Time.valueOf(startTime))
                .addValue("endTime", java.sql.Time.valueOf(endTime));

        Integer count = jdbc.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    /**
     * Xóa toàn bộ lịch rảnh của giáo viên theo ID giáo viên
     */
    public int deleteByTeacherId(Long teacherId) {
        String sql = "DELETE FROM teacher_availability WHERE teacher_id = :teacherId";
        return jdbc.update(sql, new MapSqlParameterSource("teacherId", teacherId));
    }

    /**
     * Xóa một ca lịch rảnh cụ thể theo ID
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM teacher_availability WHERE id = :id";
        return jdbc.update(sql, new MapSqlParameterSource("id", id));
    }
}
