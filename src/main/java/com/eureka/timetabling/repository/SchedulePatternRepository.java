package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.SchedulePattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý Mẫu lịch học (SchedulePattern).
 * Sử dụng NamedParameterJdbcTemplate - KHÔNG dùng JPA/Hibernate.
 */
@Repository
@RequiredArgsConstructor
public class SchedulePatternRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** RowMapper ánh xạ ResultSet → SchedulePattern, convert TIME → String HH:mm */
    private final RowMapper<SchedulePattern> patternMapper = (rs, row) -> {
        // Convert SQL Time sang String dạng HH:mm
        Time startTime = rs.getTime("slot_start");
        Time endTime = rs.getTime("slot_end");
        String slotStart = startTime != null ? startTime.toString().substring(0, 5) : null;
        String slotEnd = endTime != null ? endTime.toString().substring(0, 5) : null;

        return SchedulePattern.builder()
                .id(rs.getLong("id"))
                .code(rs.getString("code"))
                .studyDays(rs.getString("study_days"))
                .slotCode(rs.getString("slot_code"))
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .sessionsPerWeek(rs.getInt("sessions_per_week"))
                .active(rs.getBoolean("active"))
                .build();
    };

    /** Lấy tất cả pattern đang hoạt động (active=1), sắp xếp theo code */
    public List<SchedulePattern> findAllActive() {
        return jdbc.query(
                "SELECT id, code, study_days, slot_code, slot_start, slot_end, sessions_per_week, active " +
                "FROM schedule_pattern WHERE active = 1 ORDER BY code",
                new MapSqlParameterSource(), patternMapper);
    }

    /** Tìm pattern theo ID */
    public Optional<SchedulePattern> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, code, study_days, slot_code, slot_start, slot_end, sessions_per_week, active " +
                "FROM schedule_pattern WHERE id = :id",
                new MapSqlParameterSource("id", id), patternMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Tìm pattern theo mã code */
    public Optional<SchedulePattern> findByCode(String code) {
        var list = jdbc.query(
                "SELECT id, code, study_days, slot_code, slot_start, slot_end, sessions_per_week, active " +
                "FROM schedule_pattern WHERE code = :code",
                new MapSqlParameterSource("code", code), patternMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
