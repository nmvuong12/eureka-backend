package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.LeaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý đơn xin nghỉ */
@Repository
@RequiredArgsConstructor
public class LeaveRequestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<LeaveRequest> mapper = (rs, row) -> LeaveRequest.builder()
            .id(rs.getLong("id"))
            .teacherId(rs.getLong("teacher_id"))
            .fromDate(rs.getDate("from_date") != null ? rs.getDate("from_date").toLocalDate() : null)
            .toDate(rs.getDate("to_date") != null ? rs.getDate("to_date").toLocalDate() : null)
            .reason(rs.getString("reason"))
            .status(rs.getString("status"))
            .sessionType(rs.getString("session_type"))
            .makeupOption(rs.getString("makeup_option"))
            .makeupDate(rs.getDate("makeup_date") != null ? rs.getDate("makeup_date").toLocalDate() : null)
            .makeupTimeslotId(rs.getObject("makeup_timeslot_id") != null ? rs.getLong("makeup_timeslot_id") : null)
            .dayConfigs(rs.getString("day_configs"))
            .reviewedBy(rs.getObject("reviewed_by") != null ? rs.getLong("reviewed_by") : null)
            .reviewedAt(rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null)
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<LeaveRequest> findAll(String status) {
        String sql = """
                SELECT id, teacher_id, from_date, to_date, reason, status, session_type, makeup_option, makeup_date, makeup_timeslot_id,
                       day_configs, reviewed_by, reviewed_at, created_at, updated_at
                FROM leave_request
                WHERE is_deleted = 0 AND (:status IS NULL OR status = :status)
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("status", status), mapper);
    }

    public List<LeaveRequest> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT id, teacher_id, from_date, to_date, reason, status, session_type, makeup_option, makeup_date, makeup_timeslot_id,
                       day_configs, reviewed_by, reviewed_at, created_at, updated_at
                FROM leave_request WHERE teacher_id = :teacherId AND is_deleted = 0 ORDER BY created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), mapper);
    }

    public Optional<LeaveRequest> findById(Long id) {
        var list = jdbc.query("""
                SELECT id, teacher_id, from_date, to_date, reason, status, session_type, makeup_option, makeup_date, makeup_timeslot_id,
                       day_configs, reviewed_by, reviewed_at, created_at, updated_at
                FROM leave_request WHERE id = :id AND is_deleted = 0
                """, new MapSqlParameterSource("id", id), mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(LeaveRequest lr) {
        String sql = """
                INSERT INTO leave_request (teacher_id, from_date, to_date, reason, status, session_type, makeup_option, makeup_date, makeup_timeslot_id, day_configs)
                VALUES (:teacherId, :fromDate, :toDate, :reason, 'PENDING', :sessionType, :makeupOption, :makeupDate, :makeupTimeslotId, :dayConfigs)
                """;
        var params = new MapSqlParameterSource()
                .addValue("teacherId", lr.getTeacherId())
                .addValue("fromDate", lr.getFromDate())
                .addValue("toDate", lr.getToDate())
                .addValue("reason", lr.getReason())
                .addValue("sessionType", lr.getSessionType())
                .addValue("makeupOption", lr.getMakeupOption())
                .addValue("makeupDate", lr.getMakeupDate())
                .addValue("makeupTimeslotId", lr.getMakeupTimeslotId())
                .addValue("dayConfigs", lr.getDayConfigs());

        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public int updateStatus(Long id, String status, Long reviewedBy) {
        String sql = """
                UPDATE leave_request
                SET status = :status, reviewed_by = :reviewedBy, reviewed_at = NOW(), updated_at = NOW()
                WHERE id = :id AND is_deleted = 0
                """;
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status)
                .addValue("reviewedBy", reviewedBy));
    }

    public int deleteById(Long id) {
        return jdbc.update("UPDATE leave_request SET is_deleted = 1 WHERE id = :id", new MapSqlParameterSource("id", id));
    }
}
