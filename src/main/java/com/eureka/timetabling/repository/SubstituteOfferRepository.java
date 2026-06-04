package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.SubstituteOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Repository quản lý các lời mời dạy thay First-Come-First-Serve */
@Repository
@RequiredArgsConstructor
public class SubstituteOfferRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<SubstituteOffer> mapper = (rs, row) -> SubstituteOffer.builder()
            .id(rs.getLong("id"))
            .leaveRequestId(rs.getLong("leave_request_id"))
            .lessonId(rs.getLong("lesson_id"))
            .teacherId(rs.getLong("teacher_id"))
            .token(rs.getString("token"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
            .build();

    private final RowMapper<SubstituteOffer> detailedMapper = (rs, row) -> SubstituteOffer.builder()
            .id(rs.getLong("id"))
            .leaveRequestId(rs.getLong("leave_request_id"))
            .lessonId(rs.getLong("lesson_id"))
            .teacherId(rs.getLong("teacher_id"))
            .token(rs.getString("token"))
            .status(rs.getString("status"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
            .classCode(rs.getString("class_code"))
            .lessonIndex(rs.getInt("lesson_index"))
            .sessionDate(rs.getDate("session_date") != null ? rs.getDate("session_date").toLocalDate() : null)
            .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
            .timeslotLabel(rs.getString("timeslot_label"))
            .originalTeacherName(rs.getString("original_teacher_name"))
            .requiredSkill(rs.getString("required_skill"))
            .build();

    public Optional<SubstituteOffer> findByToken(String token) {
        String sql = """
                SELECT so.id, so.leave_request_id, so.lesson_id, so.teacher_id, so.token, so.status, so.created_at, so.expires_at,
                       COALESCE(c.class_code, c.name) AS class_code, l.lesson_index, la.session_date, la.timeslot_id, ts.label AS timeslot_label,
                       t_orig.full_name AS original_teacher_name, l.required_skill
                FROM substitute_offer so
                INNER JOIN lesson l ON so.lesson_id = l.id
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                INNER JOIN leave_request lr ON so.leave_request_id = lr.id
                INNER JOIN teacher t_orig ON lr.teacher_id = t_orig.id
                WHERE so.token = :token
                """;
        var list = jdbc.query(sql, new MapSqlParameterSource("token", token), detailedMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<SubstituteOffer> findById(Long id) {
        String sql = "SELECT id, leave_request_id, lesson_id, teacher_id, token, status, created_at, expires_at FROM substitute_offer WHERE id = :id";
        var list = jdbc.query(sql, new MapSqlParameterSource("id", id), mapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Long save(SubstituteOffer offer) {
        String sql = """
                INSERT INTO substitute_offer (leave_request_id, lesson_id, teacher_id, token, status, expires_at)
                VALUES (:leaveRequestId, :lessonId, :teacherId, :token, 'PENDING', :expiresAt)
                """;
        var params = new MapSqlParameterSource()
                .addValue("leaveRequestId", offer.getLeaveRequestId())
                .addValue("lessonId", offer.getLessonId())
                .addValue("teacherId", offer.getTeacherId())
                .addValue("token", offer.getToken())
                .addValue("expiresAt", offer.getExpiresAt());
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    public int updateStatus(Long id, String status) {
        String sql = "UPDATE substitute_offer SET status = :status WHERE id = :id";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("id", id).addValue("status", status));
    }

    public int expireAllOthersForLesson(Long lessonId, Long acceptedId) {
        String sql = "UPDATE substitute_offer SET status = 'EXPIRED' WHERE lesson_id = :lessonId AND id <> :acceptedId AND status = 'PENDING'";
        return jdbc.update(sql, new MapSqlParameterSource().addValue("lessonId", lessonId).addValue("acceptedId", acceptedId));
    }
}
