package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.LeaveRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Repository quản lý đơn xin nghỉ */
@Repository
@RequiredArgsConstructor
public class LeaveRequestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<LeaveRequest> mapper = (rs, row) -> {
        String teacherName = null;
        String teacherCode = null;
        try {
            teacherName = rs.getString("teacher_name");
            teacherCode = rs.getString("teacher_code");
        } catch (Exception ignored) {}

        return LeaveRequest.builder()
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
                .teacherName(teacherName)
                .teacherCode(teacherCode)
                .build();
    };

    public List<LeaveRequest> findAll(String status) {
        String sql = """
                SELECT lr.id, lr.teacher_id, lr.from_date, lr.to_date, lr.reason, lr.status, lr.session_type, 
                       lr.makeup_option, lr.makeup_date, lr.makeup_timeslot_id, lr.day_configs, 
                       lr.reviewed_by, lr.reviewed_at, lr.created_at, lr.updated_at,
                       t.full_name AS teacher_name, t.teacher_code
                FROM leave_request lr
                LEFT JOIN teacher t ON lr.teacher_id = t.id
                WHERE lr.is_deleted = 0 AND (:status IS NULL OR lr.status = :status)
                ORDER BY lr.created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("status", status), mapper);
    }

    public List<LeaveRequest> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT lr.id, lr.teacher_id, lr.from_date, lr.to_date, lr.reason, lr.status, lr.session_type, 
                       lr.makeup_option, lr.makeup_date, lr.makeup_timeslot_id, lr.day_configs, 
                       lr.reviewed_by, lr.reviewed_at, lr.created_at, lr.updated_at,
                       t.full_name AS teacher_name, t.teacher_code
                FROM leave_request lr
                LEFT JOIN teacher t ON lr.teacher_id = t.id
                WHERE lr.teacher_id = :teacherId AND lr.is_deleted = 0 
                ORDER BY lr.created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), mapper);
    }

    public Optional<LeaveRequest> findById(Long id) {
        var list = jdbc.query("""
                SELECT lr.id, lr.teacher_id, lr.from_date, lr.to_date, lr.reason, lr.status, lr.session_type, 
                       lr.makeup_option, lr.makeup_date, lr.makeup_timeslot_id, lr.day_configs, 
                       lr.reviewed_by, lr.reviewed_at, lr.created_at, lr.updated_at,
                       t.full_name AS teacher_name, t.teacher_code
                FROM leave_request lr
                LEFT JOIN teacher t ON lr.teacher_id = t.id
                WHERE lr.id = :id AND lr.is_deleted = 0
                """, new MapSqlParameterSource("id", id), mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<LeaveRequest> search(Long teacherId, String teacherName, LocalDate fromDate, LocalDate toDate, String makeupOption, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT lr.id, lr.teacher_id, lr.from_date, lr.to_date, lr.reason, lr.status, lr.session_type, 
                       lr.makeup_option, lr.makeup_date, lr.makeup_timeslot_id, lr.day_configs, 
                       lr.reviewed_by, lr.reviewed_at, lr.created_at, lr.updated_at,
                       t.full_name AS teacher_name, t.teacher_code
                FROM leave_request lr
                LEFT JOIN teacher t ON lr.teacher_id = t.id
                WHERE lr.is_deleted = 0
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (teacherId != null) {
            sql.append(" AND lr.teacher_id = :teacherId");
            params.addValue("teacherId", teacherId);
        }
        if (teacherName != null && !teacherName.isBlank()) {
            sql.append(" AND (LOWER(t.full_name) LIKE LOWER(:teacherName) OR LOWER(t.teacher_code) LIKE LOWER(:teacherName))");
            params.addValue("teacherName", "%" + teacherName.trim().toLowerCase() + "%");
        }
        if (fromDate != null) {
            sql.append(" AND lr.from_date >= :fromDate");
            params.addValue("fromDate", java.sql.Date.valueOf(fromDate));
        }
        if (toDate != null) {
            sql.append(" AND lr.to_date <= :toDate");
            params.addValue("toDate", java.sql.Date.valueOf(toDate));
        }
        if (makeupOption != null && !makeupOption.isBlank()) {
            sql.append(" AND lr.makeup_option = :makeupOption");
            params.addValue("makeupOption", makeupOption);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND lr.status = :status");
            params.addValue("status", status);
        }

        sql.append(" ORDER BY lr.created_at DESC");
        return jdbc.query(sql.toString(), params, mapper);
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
