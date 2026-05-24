package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.ClassPlanningLog;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository lưu trữ audit log cho state machine lớp học.
 * Sử dụng NamedParameterJdbcTemplate - KHÔNG dùng JPA/Hibernate.
 */
@Repository
@RequiredArgsConstructor
public class ClassPlanningLogRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** RowMapper ánh xạ ResultSet → ClassPlanningLog */
    private final RowMapper<ClassPlanningLog> logMapper = (rs, row) -> ClassPlanningLog.builder()
            .id(rs.getLong("id"))
            .classId(rs.getLong("class_id"))
            .action(rs.getString("action"))
            .oldStatus(rs.getString("old_status"))
            .newStatus(rs.getString("new_status"))
            .note(rs.getString("note"))
            .createdAt(rs.getTimestamp("created_at") != null
                    ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .createdBy(rs.getString("created_by"))
            .build();

    /** Lưu một bản ghi audit log */
    public void save(ClassPlanningLog log) {
        String sql = """
                INSERT INTO class_planning_log (class_id, action, old_status, new_status, note, created_by)
                VALUES (:classId, :action, :oldStatus, :newStatus, :note, :createdBy)
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("classId", log.getClassId())
                .addValue("action", log.getAction())
                .addValue("oldStatus", log.getOldStatus())
                .addValue("newStatus", log.getNewStatus())
                .addValue("note", log.getNote())
                .addValue("createdBy", log.getCreatedBy()));
    }

    /** Lấy toàn bộ audit log của một lớp học, sắp xếp mới nhất trước */
    public List<ClassPlanningLog> findByClassId(Long classId) {
        String sql = """
                SELECT id, class_id, action, old_status, new_status, note, created_at, created_by
                FROM class_planning_log
                WHERE class_id = :classId
                ORDER BY created_at DESC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("classId", classId), logMapper);
    }
}
