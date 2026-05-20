package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Skill;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SkillRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<Skill> mapper = (rs, rowNum) -> Skill.builder()
            .id(rs.getLong("id"))
            .skillCode(rs.getString("skill_code"))
            .skillName(rs.getString("skill_name"))
            .description(rs.getString("description"))
            .isDeleted(rs.getInt("is_deleted"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Skill> search(String query, int page, int size) {
        String sql = """
                SELECT id, skill_code, skill_name, description, is_deleted, created_at, updated_at
                FROM skill
                WHERE is_deleted = 0
                  AND (LOWER(skill_code) LIKE LOWER(:query) OR LOWER(skill_name) LIKE LOWER(:query))
                ORDER BY skill_name
                LIMIT :limit OFFSET :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", "%" + (query != null ? query.trim() : "") + "%")
                .addValue("limit", size)
                .addValue("offset", (page - 1) * size);
        return jdbc.query(sql, params, mapper);
    }

    public long countSearch(String query) {
        String sql = """
                SELECT COUNT(*) FROM skill
                WHERE is_deleted = 0
                  AND (LOWER(skill_code) LIKE LOWER(:query) OR LOWER(skill_name) LIKE LOWER(:query))
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("query", "%" + (query != null ? query.trim() : "") + "%");
        Long count = jdbc.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    public List<Skill> findAll() {
        return jdbc.query("SELECT * FROM skill WHERE is_deleted = 0 ORDER BY skill_name", mapper);
    }

    public Optional<Skill> findById(Long id) {
        String sql = "SELECT * FROM skill WHERE id = :id AND is_deleted = 0";
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM skill WHERE skill_code = :code AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("code", code), Integer.class);
        return count != null && count > 0;
    }

    public Long save(Skill skill) {
        String sql = """
                INSERT INTO skill (skill_code, skill_name, description)
                VALUES (:skillCode, :skillName, :description)
                """;
        var params = new MapSqlParameterSource()
                .addValue("skillCode", skill.getSkillCode())
                .addValue("skillName", skill.getSkillName())
                .addValue("description", skill.getDescription());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public int update(Skill skill) {
        String sql = """
                UPDATE skill
                SET skill_code = :skillCode, skill_name = :skillName, description = :description, updated_at = NOW()
                WHERE id = :id AND is_deleted = 0
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", skill.getId())
                .addValue("skillCode", skill.getSkillCode())
                .addValue("skillName", skill.getSkillName())
                .addValue("description", skill.getDescription());
        return jdbc.update(sql, params);
    }

    public int deleteById(Long id) {
        return jdbc.update("UPDATE skill SET is_deleted = 1 WHERE id = :id",
                new MapSqlParameterSource("id", id));
    }
}
