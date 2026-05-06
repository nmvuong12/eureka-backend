package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Course;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý khóa học */
@Repository
@RequiredArgsConstructor
public class CourseRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<Course> courseMapper = (rs, row) -> Course.builder()
            .id(rs.getLong("id"))
            .name(rs.getString("name"))
            .totalLessons(rs.getInt("total_lessons"))
            .defaultDuration(rs.getInt("default_duration"))
            .description(rs.getString("description"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public List<Course> findAll() {
        return jdbc.query(
                "SELECT id, name, total_lessons, default_duration, description, created_at, updated_at FROM course ORDER BY name",
                new MapSqlParameterSource(), courseMapper);
    }

    public Optional<Course> findById(Long id) {
        var list = jdbc.query(
                "SELECT id, name, total_lessons, default_duration, description, created_at, updated_at FROM course WHERE id = :id",
                new MapSqlParameterSource("id", id), courseMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(Course course) {
        String sql = "INSERT INTO course (name, total_lessons, default_duration, description) VALUES (:name, :total, :duration, :desc)";
        var params = new MapSqlParameterSource()
                .addValue("name", course.getName())
                .addValue("total", course.getTotalLessons())
                .addValue("duration", course.getDefaultDuration())
                .addValue("desc", course.getDescription());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public int update(Course course) {
        String sql = "UPDATE course SET name=:name, total_lessons=:total, default_duration=:duration, description=:desc, updated_at=NOW() WHERE id=:id";
        return jdbc.update(sql, new MapSqlParameterSource()
                .addValue("id", course.getId())
                .addValue("name", course.getName())
                .addValue("total", course.getTotalLessons())
                .addValue("duration", course.getDefaultDuration())
                .addValue("desc", course.getDescription()));
    }

    public int deleteById(Long id) {
        return jdbc.update("DELETE FROM course WHERE id = :id", new MapSqlParameterSource("id", id));
    }
}
