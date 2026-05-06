package com.eureka.timetabling.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Repository quản lý tài khoản người dùng */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record UserRecord(Long id, String username, String passwordHash, String role,
                              Long teacherId, boolean isActive) {}

    public Optional<UserRecord> findByUsername(String username) {
        String sql = """
                SELECT id, username, password_hash, role, teacher_id, is_active
                FROM user_account WHERE username = :username
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("username", username),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<UserRecord> findById(Long id) {
        String sql = "SELECT id, username, password_hash, role, teacher_id, is_active FROM user_account WHERE id = :id";
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(String username, String passwordHash, String role, Long teacherId) {
        String sql = """
                INSERT INTO user_account (username, password_hash, role, teacher_id)
                VALUES (:username, :passwordHash, :role, :teacherId)
                """;
        var params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("passwordHash", passwordHash)
                .addValue("role", role)
                .addValue("teacherId", teacherId);
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public Optional<UserRecord> findByTeacherId(Long teacherId) {
        String sql = "SELECT id, username, password_hash, role, teacher_id, is_active FROM user_account WHERE teacher_id = :teacherId";
        var list = jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public java.util.List<UserRecord> findAll() {
        String sql = "SELECT id, username, password_hash, role, teacher_id, is_active FROM user_account ORDER BY id DESC";
        return jdbc.query(sql, (rs, row) -> new UserRecord(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                rs.getBoolean("is_active")));
    }

    public void update(Long id, String role, boolean isActive, Long teacherId) {
        String sql = """
                UPDATE user_account 
                SET role = :role, is_active = :isActive, teacher_id = :teacherId 
                WHERE id = :id
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("role", role)
                .addValue("isActive", isActive)
                .addValue("teacherId", teacherId);
        jdbc.update(sql, params);
    }

    public void updatePassword(Long id, String passwordHash) {
        String sql = "UPDATE user_account SET password_hash = :passwordHash WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id).addValue("passwordHash", passwordHash));
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM user_account WHERE id = :id", new MapSqlParameterSource("id", id));
    }
}

