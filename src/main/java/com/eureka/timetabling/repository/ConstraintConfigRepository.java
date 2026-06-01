package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.ConstraintConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý cấu hình các ràng buộc giảng dạy sử dụng NamedParameterJdbcTemplate.
 */
@Repository
@RequiredArgsConstructor
public class ConstraintConfigRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<ConstraintConfig> rowMapper = (rs, rowNum) -> ConstraintConfig.builder()
            .id(rs.getLong("id"))
            .constraintKey(rs.getString("constraint_key"))
            .constraintName(rs.getString("constraint_name"))
            .description(rs.getString("description"))
            .enabled(rs.getInt("is_enabled") == 1)
            .weight(rs.getInt("weight"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    /**
     * Lấy toàn bộ danh sách cấu hình ràng buộc
     */
    public List<ConstraintConfig> findAll() {
        String sql = """
                SELECT id, constraint_key, constraint_name, description, is_enabled, weight, created_at, updated_at
                FROM constraint_config
                ORDER BY id ASC
                """;
        return jdbc.query(sql, rowMapper);
    }

    /**
     * Tìm cấu hình ràng buộc theo Key
     */
    public ConstraintConfig findByKey(String constraintKey) {
        String sql = """
                SELECT id, constraint_key, constraint_name, description, is_enabled, weight, created_at, updated_at
                FROM constraint_config
                WHERE constraint_key = :constraintKey
                """;
        List<ConstraintConfig> list = jdbc.query(sql, new MapSqlParameterSource("constraintKey", constraintKey), rowMapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Cập nhật trạng thái bật/tắt và trọng số của một ràng buộc
     */
    public void update(ConstraintConfig config) {
        String sql = """
                UPDATE constraint_config
                SET is_enabled = :enabled,
                    weight = :weight,
                    updated_at = NOW()
                WHERE constraint_key = :constraintKey
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("enabled", config.isEnabled() ? 1 : 0)
                .addValue("weight", config.getWeight())
                .addValue("constraintKey", config.getConstraintKey()));
    }
}
