package com.eureka.timetabling.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/** Repository quản lý tài khoản người dùng */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public record UserRecord(Long id, String username, String passwordHash, String role,
                              Long teacherId, boolean isActive,
                              String fullName, String gender, LocalDate dob,
                              String address, String phone, String email) {}

    public Optional<UserRecord> findByUsername(String username) {
        String sql = """
                SELECT ua.id, ua.username, ua.password_hash, ua.role, ua.teacher_id, ua.is_active,
                       COALESCE(t.name, ua.full_name) as full_name, ua.gender, ua.dob, ua.address, ua.phone,
                       COALESCE(t.email, ua.email) as email
                FROM user_account ua
                LEFT JOIN teacher t ON ua.teacher_id = t.id
                WHERE ua.username = :username AND ua.is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("username", username),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getObject("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                        rs.getString("address"),
                        rs.getString("phone"),
                        rs.getString("email")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<UserRecord> findById(Long id) {
        String sql = """
                SELECT ua.id, ua.username, ua.password_hash, ua.role, ua.teacher_id, ua.is_active,
                       COALESCE(t.name, ua.full_name) as full_name, ua.gender, ua.dob, ua.address, ua.phone,
                       COALESCE(t.email, ua.email) as email
                FROM user_account ua
                LEFT JOIN teacher t ON ua.teacher_id = t.id
                WHERE ua.id = :id AND ua.is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getObject("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                        rs.getString("address"),
                        rs.getString("phone"),
                        rs.getString("email")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(String username, String passwordHash, String role, Long teacherId,
                     String fullName, String gender, LocalDate dob, String address, String phone, String email) {
        String sql = """
                INSERT INTO user_account (username, password_hash, role, teacher_id, full_name, gender, dob, address, phone, email)
                VALUES (:username, :passwordHash, :role, :teacherId, :fullName, :gender, :dob, :address, :phone, :email)
                """;
        var params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("passwordHash", passwordHash)
                .addValue("role", role)
                .addValue("teacherId", teacherId)
                .addValue("fullName", fullName)
                .addValue("gender", gender)
                .addValue("dob", dob)
                .addValue("address", address)
                .addValue("phone", phone)
                .addValue("email", email);
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public Optional<UserRecord> findByTeacherId(Long teacherId) {
        String sql = """
                SELECT ua.id, ua.username, ua.password_hash, ua.role, ua.teacher_id, ua.is_active,
                       COALESCE(t.name, ua.full_name) as full_name, ua.gender, ua.dob, ua.address, ua.phone,
                       COALESCE(t.email, ua.email) as email
                FROM user_account ua
                LEFT JOIN teacher t ON ua.teacher_id = t.id
                WHERE ua.teacher_id = :teacherId AND ua.is_deleted = 0
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId),
                (rs, row) -> new UserRecord(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                        rs.getBoolean("is_active"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getObject("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                        rs.getString("address"),
                        rs.getString("phone"),
                        rs.getString("email")));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public java.util.List<UserRecord> search(String username, String role, Boolean isActive, String fullName, String email, int page, int size) {
        StringBuilder sql = new StringBuilder("""
                SELECT ua.id, ua.username, ua.password_hash, ua.role, ua.teacher_id, ua.is_active,
                       COALESCE(t.name, ua.full_name) as full_name, ua.gender, ua.dob, ua.address, ua.phone,
                       COALESCE(t.email, ua.email) as email
                FROM user_account ua
                LEFT JOIN teacher t ON ua.teacher_id = t.id
                WHERE ua.is_deleted = 0
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(ua.username) LIKE LOWER(:username) ");
            params.addValue("username", "%" + username.trim() + "%");
        }
        if (role != null && !role.isBlank()) {
            sql.append(" AND ua.role = :role ");
            params.addValue("role", role);
        }
        if (isActive != null) {
            sql.append(" AND ua.is_active = :isActive ");
            params.addValue("isActive", isActive);
        }
        if (fullName != null && !fullName.isBlank()) {
            sql.append(" AND (LOWER(ua.full_name) LIKE LOWER(:fullName) OR LOWER(t.name) LIKE LOWER(:fullName)) ");
            params.addValue("fullName", "%" + fullName.trim() + "%");
        }
        if (email != null && !email.isBlank()) {
            sql.append(" AND (LOWER(ua.email) LIKE LOWER(:email) OR LOWER(t.email) LIKE LOWER(:email)) ");
            params.addValue("email", "%" + email.trim() + "%");
        }

        sql.append(" ORDER BY ua.id DESC LIMIT :limit OFFSET :offset");
        params.addValue("limit", size);
        params.addValue("offset", (page - 1) * size);

        return jdbc.query(sql.toString(), params, (rs, row) -> new UserRecord(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null,
                rs.getBoolean("is_active"),
                rs.getString("full_name"),
                rs.getString("gender"),
                rs.getObject("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                rs.getString("address"),
                rs.getString("phone"),
                rs.getString("email")));
    }

    public long countSearch(String username, String role, Boolean isActive, String fullName, String email) {
        StringBuilder sql = new StringBuilder("""
                SELECT count(*) FROM user_account ua
                LEFT JOIN teacher t ON ua.teacher_id = t.id
                WHERE ua.is_deleted = 0
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (username != null && !username.isBlank()) {
            sql.append(" AND LOWER(ua.username) LIKE LOWER(:username) ");
            params.addValue("username", "%" + username.trim() + "%");
        }
        if (role != null && !role.isBlank()) {
            sql.append(" AND ua.role = :role ");
            params.addValue("role", role);
        }
        if (isActive != null) {
            sql.append(" AND ua.is_active = :isActive ");
            params.addValue("isActive", isActive);
        }
        if (fullName != null && !fullName.isBlank()) {
            sql.append(" AND (LOWER(ua.full_name) LIKE LOWER(:fullName) OR LOWER(t.name) LIKE LOWER(:fullName)) ");
            params.addValue("fullName", "%" + fullName.trim() + "%");
        }
        if (email != null && !email.isBlank()) {
            sql.append(" AND (LOWER(ua.email) LIKE LOWER(:email) OR LOWER(t.email) LIKE LOWER(:email)) ");
            params.addValue("email", "%" + email.trim() + "%");
        }

        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    public void update(Long id, String role, boolean isActive, Long teacherId,
                       String fullName, String gender, LocalDate dob, String address, String phone, String email) {
        String sql = """
                UPDATE user_account 
                SET role = :role, is_active = :isActive, teacher_id = :teacherId,
                    full_name = :fullName, gender = :gender, dob = :dob, 
                    address = :address, phone = :phone, email = :email
                WHERE id = :id
                """;
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("role", role)
                .addValue("isActive", isActive)
                .addValue("teacherId", teacherId)
                .addValue("fullName", fullName)
                .addValue("gender", gender)
                .addValue("dob", dob)
                .addValue("address", address)
                .addValue("phone", phone)
                .addValue("email", email);
        jdbc.update(sql, params);
    }

    public void updatePassword(Long id, String passwordHash) {
        String sql = "UPDATE user_account SET password_hash = :passwordHash WHERE id = :id";
        jdbc.update(sql, new MapSqlParameterSource("id", id).addValue("passwordHash", passwordHash));
    }

    public void deleteById(Long id) {
        jdbc.update("UPDATE user_account SET is_deleted = 1 WHERE id = :id", new MapSqlParameterSource("id", id));
    }
}

