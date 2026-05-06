package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Room;
import com.eureka.timetabling.solver.Timetable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý phòng học */
@Repository
@RequiredArgsConstructor
public class RoomRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<Room> roomMapper = (rs, row) -> Room.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .capacity(rs.getInt("capacity"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Room> findAll() {
        return jdbc.query(
                "SELECT id, name, capacity, status, created_at, updated_at FROM room ORDER BY name",
                new MapSqlParameterSource(), roomMapper);
    }

    public Optional<Room> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, name, capacity, status, created_at, updated_at FROM room WHERE id = :id",
                new MapSqlParameterSource("id", id), roomMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(Room room) {
        String sql = "INSERT INTO room (name, capacity, status) VALUES (:name, :capacity, :status)";
        var params = new MapSqlParameterSource()
                .addValue("name", room.getName())
                .addValue("capacity", room.getCapacity())
                .addValue("status", room.getStatus() != null ? room.getStatus() : "ACTIVE");
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public int update(Room room) {
        String sql = "UPDATE room SET name = :name, capacity = :capacity, status = :status, updated_at = NOW() WHERE id = :id";
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", room.getId())
                .addValue("name", room.getName())
                .addValue("capacity", room.getCapacity())
                .addValue("status", room.getStatus()));
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM room WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<Long> findAllActiveIds() {
        return jdbc.queryForList("SELECT id FROM room WHERE status = 'ACTIVE'",
                new MapSqlParameterSource(), Long.class);
    }

    public List<Timetable.RoomFact> findAllRoomFacts() {
        String sql = "SELECT id, capacity, status FROM room";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.RoomFact(rs.getLong("id"), rs.getInt("capacity"), rs.getString("status")));
    }
}
