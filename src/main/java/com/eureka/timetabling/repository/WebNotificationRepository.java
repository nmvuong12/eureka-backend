package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.WebNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository quản lý các thông báo đẩy hiển thị trên Web */
@Repository
@RequiredArgsConstructor
public class WebNotificationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<WebNotification> mapper = (rs, row) -> WebNotification.builder()
            .id(rs.getLong("id"))
            .recipientRole(rs.getString("recipient_role"))
            .message(rs.getString("message"))
            .isRead(rs.getBoolean("is_read"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .build();

    public Long save(WebNotification notification) {
        String sql = """
                INSERT INTO web_notification (recipient_role, message, is_read)
                VALUES (:recipientRole, :message, 0)
                """;
        var params = new MapSqlParameterSource()
                .addValue("recipientRole", notification.getRecipientRole())
                .addValue("message", notification.getMessage());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public List<WebNotification> findByRole(String role, int limit) {
        String sql = """
                SELECT id, recipient_role, message, is_read, created_at
                FROM web_notification
                WHERE recipient_role = :role
                ORDER BY created_at DESC
                LIMIT :limit
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                .addValue("role", role)
                .addValue("limit", limit), mapper);
    }

    public int markAsRead(Long id) {
        String sql = "UPDATE web_notification SET is_read = 1 WHERE id = :id";
        return jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    public int markAllAsRead(String role) {
        String sql = "UPDATE web_notification SET is_read = 1 WHERE recipient_role = :role AND is_read = 0";
        return jdbc.update(sql, new MapSqlParameterSource("role", role));
    }

    public int countUnreadByRole(String role) {
        String sql = "SELECT COUNT(*) FROM web_notification WHERE recipient_role = :role AND is_read = 0";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("role", role), Integer.class);
        return count != null ? count : 0;
    }
}
