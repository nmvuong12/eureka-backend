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
                "SELECT id, name, capacity, status, created_at, updated_at FROM room WHERE is_deleted = 0 ORDER BY name",
                new MapSqlParameterSource(), roomMapper);
    }

    public Optional<Room> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, name, capacity, status, created_at, updated_at FROM room WHERE id = :id AND is_deleted = 0",
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
        return jdbc.update("UPDATE room SET is_deleted = 1 WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    public List<Long> findAllActiveIds() {
        return jdbc.queryForList("SELECT id FROM room WHERE status = 'ACTIVE' AND is_deleted = 0",
                new MapSqlParameterSource(), Long.class);
    }

    public List<Timetable.RoomFact> findAllRoomFacts() {
        String sql = "SELECT id, capacity, status FROM room WHERE is_deleted = 0";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.RoomFact(rs.getLong("id"), rs.getInt("capacity"), rs.getString("status")));
    }

    /**
     * Đếm số phòng ACTIVE không nằm trong danh sách đã bị bận.
     * Dùng để tính availableRooms trong capacity dashboard.
     * @param excludeIds danh sách room_id đang bận (có thể rỗng)
     */
    public int countAvailableRooms(List<Long> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return countAllActive();
        }
        String sql = "SELECT COUNT(*) FROM room WHERE status = 'ACTIVE' AND is_deleted = 0 AND id NOT IN (:excludeIds)";
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("excludeIds", excludeIds), Integer.class);
        return count != null ? count : 0;
    }

    /** Đếm tổng số phòng ACTIVE (dùng khi không có phòng nào bị bận) */
    public int countAllActive() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM room WHERE status = 'ACTIVE' AND is_deleted = 0",
                new MapSqlParameterSource(), Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Tìm danh sách phòng học trống vào các ca/ngày dạy bù và đủ sức chứa.
     */
    public List<Room> findAvailableRoomsForSlots(int maxStudents, List<java.util.Map<String, Object>> slots) {
        if (slots == null || slots.isEmpty()) {
            return jdbc.query("SELECT id, name, capacity, status, created_at, updated_at FROM room WHERE status = 'ACTIVE' AND is_deleted = 0 ORDER BY name",
                    new MapSqlParameterSource(), roomMapper);
        }

        StringBuilder sql = new StringBuilder("""
                SELECT r.id, r.name, r.capacity, r.status, r.created_at, r.updated_at
                FROM room r
                WHERE r.status = 'ACTIVE' AND r.is_deleted = 0
                  AND r.capacity >= :maxStudents
                """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("maxStudents", maxStudents);

        sql.append("""
                  AND r.id NOT IN (
                    SELECT la.room_id
                    FROM lesson_assignment la
                    INNER JOIN lesson le ON la.lesson_id = le.id
                    INNER JOIN class c ON le.class_id = c.id
                    WHERE la.room_id IS NOT NULL AND c.is_deleted = 0 AND le.is_deleted = 0
                      AND (
                """);

        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            java.util.Map<String, Object> slot = slots.get(i);
            String dateParam = "date_" + i;
            String tsParam = "ts_" + i;
            params.addValue(dateParam, slot.get("date"))
                  .addValue(tsParam, slot.get("timeslotId"));
            sql.append("(la.session_date = :").append(dateParam).append(" AND la.timeslot_id = :").append(tsParam).append(")");
        }

        sql.append("""
                      )
                  )
                ORDER BY r.capacity ASC, r.name ASC
                """);

        return jdbc.query(sql.toString(), params, roomMapper);
    }
}
