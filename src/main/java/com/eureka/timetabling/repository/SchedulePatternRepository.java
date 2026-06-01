package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.SchedulePattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.util.List;
import java.util.Objects;
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

    /** Lấy tất cả danh sách mẫu lịch (cả đang hoạt động và khóa) */
    public List<SchedulePattern> findAll() {
        return jdbc.query(
                "SELECT id, code, study_days, slot_code, slot_start, slot_end, sessions_per_week, active " +
                "FROM schedule_pattern ORDER BY code",
                new MapSqlParameterSource(), patternMapper);
    }

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

    /** Lưu mẫu lịch học mới */
    public Long save(SchedulePattern pattern) {
        String sql = "INSERT INTO schedule_pattern (code, study_days, slot_code, slot_start, slot_end, sessions_per_week, active) " +
                "VALUES (:code, :studyDays, :slotCode, :slotStart, :slotEnd, :sessionsPerWeek, :active)";
        
        // Cần đảm bảo định dạng giờ là HH:mm:ss khi lưu xuống DB
        String slotStart = pattern.getSlotStart().contains(":") && pattern.getSlotStart().split(":").length == 2 
                ? pattern.getSlotStart() + ":00" : pattern.getSlotStart();
        String slotEnd = pattern.getSlotEnd().contains(":") && pattern.getSlotEnd().split(":").length == 2 
                ? pattern.getSlotEnd() + ":00" : pattern.getSlotEnd();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("code", pattern.getCode())
                .addValue("studyDays", pattern.getStudyDays())
                .addValue("slotCode", pattern.getSlotCode())
                .addValue("slotStart", slotStart)
                .addValue("slotEnd", slotEnd)
                .addValue("sessionsPerWeek", pattern.getSessionsPerWeek())
                .addValue("active", pattern.getActive() ? 1 : 0);

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /** Cập nhật thông tin mẫu lịch học */
    public void update(SchedulePattern pattern) {
        String sql = "UPDATE schedule_pattern SET study_days = :studyDays, slot_code = :slotCode, " +
                "slot_start = :slotStart, slot_end = :slotEnd, sessions_per_week = :sessionsPerWeek, active = :active " +
                "WHERE id = :id";

        String slotStart = pattern.getSlotStart().contains(":") && pattern.getSlotStart().split(":").length == 2 
                ? pattern.getSlotStart() + ":00" : pattern.getSlotStart();
        String slotEnd = pattern.getSlotEnd().contains(":") && pattern.getSlotEnd().split(":").length == 2 
                ? pattern.getSlotEnd() + ":00" : pattern.getSlotEnd();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", pattern.getId())
                .addValue("studyDays", pattern.getStudyDays())
                .addValue("slotCode", pattern.getSlotCode())
                .addValue("slotStart", slotStart)
                .addValue("slotEnd", slotEnd)
                .addValue("sessionsPerWeek", pattern.getSessionsPerWeek())
                .addValue("active", pattern.getActive() ? 1 : 0);

        jdbc.update(sql, params);
    }

    /** Xóa mẫu lịch học theo ID */
    public void deleteById(Long id) {
        String sql = "DELETE FROM schedule_pattern WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    /** Tự động sinh mã code tiếp theo (Ví dụ: P020) */
    public String getNextAvailableCode() {
        String sql = "SELECT code FROM schedule_pattern ORDER BY id DESC LIMIT 1";
        List<String> codes = jdbc.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> rs.getString("code"));
        if (codes.isEmpty()) {
            return "P001";
        }
        String lastCode = codes.get(0);
        try {
            if (lastCode.startsWith("P")) {
                int num = Integer.parseInt(lastCode.substring(1));
                return String.format("P%03d", num + 1);
            }
        } catch (Exception ignored) {
        }
        return "P" + java.util.UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    /** Kiểm tra xem mẫu lịch học có đang được lớp học nào sử dụng hay không */
    public boolean isReferenced(Long id) {
        String sql = "SELECT COUNT(*) FROM class WHERE schedule_pattern_id = :id";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("id", id), Integer.class);
        return count != null && count > 0;
    }
}
