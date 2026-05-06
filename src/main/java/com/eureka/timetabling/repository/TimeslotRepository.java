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
                ORDER BY FIELD(day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), start_time
                """;
        return jdbc.query(sql, new MapSqlParameterSource(), timeslotMapper);
    }

    public Optional<Timeslot> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, day_of_week, start_time, end_time, label FROM timeslot WHERE id = :id",
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

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM timeslot WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<Long> findAllIds() {
        return jdbc.queryForList("SELECT id FROM timeslot ORDER BY id",
                new MapSqlParameterSource(), Long.class);
    }

    public List<Timetable.TimeslotFact> findAllTimeslotFacts() {
        String sql = "SELECT id, day_of_week, start_time, end_time FROM timeslot";
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
}
