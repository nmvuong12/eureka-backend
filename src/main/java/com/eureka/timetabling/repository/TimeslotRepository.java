package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Timeslot;
import com.eureka.timetabling.solver.Timetable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý ca học */
@Repository
@RequiredArgsConstructor
public class TimeslotRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<Timeslot> timeslotMapper = (rs, row) -> Timeslot.builder()
            .id(rs.getLong("id"))
            .dayOfWeek(rs.getString("day_of_week"))
            .startTime(rs.getString("start_time"))
            .endTime(rs.getString("end_time"))
            .label(rs.getString("label"))
            .build();

    public List<Timeslot> findAll() {
        String sql = """
                SELECT id, day_of_week, start_time, end_time, label FROM timeslot
                WHERE is_deleted = 0
                ORDER BY FIELD(day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), start_time
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), timeslotMapper);
    }

    public Optional<Timeslot> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, day_of_week, start_time, end_time, label FROM timeslot WHERE id = :id AND is_deleted = 0",
                new MapSqlParameterSource("id", id), timeslotMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(Timeslot timeslot) {
        String sql = "INSERT INTO timeslot (day_of_week, start_time, end_time, label) VALUES (:dayOfWeek, :startTime, :endTime, :label)";
        var params = new MapSqlParameterSource()
                .addValue("dayOfWeek", timeslot.getDayOfWeek())
                .addValue("startTime", timeslot.getStartTime())
                .addValue("endTime", timeslot.getEndTime())
                .addValue("label", timeslot.getLabel());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public void update(Timeslot timeslot) {
        String sql = "UPDATE timeslot SET day_of_week = :dayOfWeek, start_time = :startTime, " +
                "end_time = :endTime, label = :label WHERE id = :id";
        var params = new MapSqlParameterSource()
                .addValue("id", timeslot.getId())
                .addValue("dayOfWeek", timeslot.getDayOfWeek())
                .addValue("startTime", timeslot.getStartTime())
                .addValue("endTime", timeslot.getEndTime())
                .addValue("label", timeslot.getLabel());
        jdbc.update(sql, params);
    }

    public int deleteById(Long id) {
        return jdbc.update("UPDATE timeslot SET is_deleted = 1 WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<Long> findAllIds() {
        return jdbc.queryForList("SELECT id FROM timeslot WHERE is_deleted = 0 ORDER BY id",
                new MapSqlParameterSource(), Long.class);
    }

    public List<Timetable.TimeslotFact> findAllTimeslotFacts() {
        String sql = "SELECT id, day_of_week, start_time, end_time FROM timeslot WHERE is_deleted = 0";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.TimeslotFact(
                        rs.getLong("id"),
                        rs.getString("day_of_week"),
                        rs.getString("start_time"),
                        rs.getString("end_time")));
    }

    public List<Timetable.TeacherUnavailableFact> findAllTeacherUnavailableFacts() {
        String sql = "SELECT teacher_id, timeslot_id FROM teacher_unavailable";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.TeacherUnavailableFact(rs.getLong("teacher_id"), rs.getLong("timeslot_id")));
    }

    /** Kiểm tra xem ca học chuẩn có đang bị ràng buộc bởi Mẫu lịch học hoặc Lớp học thực tế nào không */
    public boolean isReferencedByPatternOrClass(Long id) {
        // 1. Kiểm tra xem ca học này có đang được xếp cho buổi học thực tế nào không (lesson_assignment)
        String sqlLesson = "SELECT COUNT(*) FROM lesson_assignment WHERE timeslot_id = :id";
        Integer lessonCount = jdbc.queryForObject(sqlLesson, new MapSqlParameterSource("id", id), Integer.class);
        if (lessonCount != null && lessonCount > 0) {
            return true;
        }

        // 2. Lấy thông tin day_of_week và label (để lấy mã ca C1-C5) của timeslot này
        String sqlTimeslot = "SELECT day_of_week, label FROM timeslot WHERE id = :id";
        var list = jdbc.query(sqlTimeslot, new MapSqlParameterSource("id", id), (rs, rowNum) -> new Object[]{
                rs.getString("day_of_week"),
                rs.getString("label")
        });
        if (list.isEmpty()) {
            return false;
        }
        
        String dayOfWeek = (String) list.get(0)[0];
        String label = (String) list.get(0)[1];
        
        // Chuyển dayOfWeek (MONDAY -> 2)
        String dayNum = "";
        switch (dayOfWeek.toUpperCase()) {
            case "MONDAY": dayNum = "2"; break;
            case "TUESDAY": dayNum = "3"; break;
            case "WEDNESDAY": dayNum = "4"; break;
            case "THURSDAY": dayNum = "5"; break;
            case "FRIDAY": dayNum = "6"; break;
            case "SATURDAY": dayNum = "7"; break;
            case "SUNDAY": dayNum = "1"; break;
        }
        
        // Trích xuất mã ca (Ca 1 -> C1)
        String slotCode = "";
        if (label != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Ca\\s*(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(label);
            if (m.find()) {
                slotCode = "C" + m.group(1);
            }
        }
        
        if (dayNum.isEmpty() || slotCode.isEmpty()) {
            return false;
        }

        // 3. Kiểm tra xem có bất kỳ Mẫu lịch học (schedule_pattern) nào đang kích hoạt mà chứa Thứ và Ca học này không
        String sqlPattern = """
                SELECT COUNT(*) FROM schedule_pattern 
                WHERE active = 1 
                  AND FIND_IN_SET(:dayNum, REPLACE(study_days, ' ', '')) > 0 
                  AND slot_code = :slotCode
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("dayNum", dayNum)
                .addValue("slotCode", slotCode);
        Integer patternCount = jdbc.queryForObject(sqlPattern, params, Integer.class);
        
        return patternCount != null && patternCount > 0;
    }
}
