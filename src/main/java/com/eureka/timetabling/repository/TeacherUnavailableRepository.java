package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.TeacherUnavailable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository quản lý lịch bận cố định của giáo viên */
@Repository
@RequiredArgsConstructor
public class TeacherUnavailableRepository {

    private final NamedParameterJdbcTemplate jdbc;

    private final RowMapper<TeacherUnavailable> unavailableMapper = (rs, row) -> TeacherUnavailable.builder()
            .id(rs.getLong("id"))
            .teacherId(rs.getLong("teacher_id"))
            .timeslotId(rs.getLong("timeslot_id"))
            .reason(rs.getString("reason"))
            .build();

    /** Lấy danh sách tất cả các ID ca học bận của một giáo viên */
    public List<Long> findTimeslotIdsByTeacherId(Long teacherId) {
        String sql = "SELECT timeslot_id FROM teacher_unavailable WHERE teacher_id = :teacherId";
        return jdbc.queryForList(sql, new MapSqlParameterSource("teacherId", teacherId), Long.class);
    }

    /** Lấy danh sách thực thể bận chi tiết của một giáo viên */
    public List<TeacherUnavailable> findByTeacherId(Long teacherId) {
        String sql = "SELECT id, teacher_id, timeslot_id, reason FROM teacher_unavailable WHERE teacher_id = :teacherId";
        return jdbc.query(sql, new MapSqlParameterSource("teacherId", teacherId), unavailableMapper);
    }

    /** Xóa toàn bộ lịch bận của giáo viên */
    public int deleteByTeacherId(Long teacherId) {
        String sql = "DELETE FROM teacher_unavailable WHERE teacher_id = :teacherId";
        return jdbc.update(sql, new MapSqlParameterSource("teacherId", teacherId));
    }

    /** Lưu đơn lẻ một lịch bận */
    public Long save(Long teacherId, Long timeslotId, String reason) {
        String sql = "INSERT INTO teacher_unavailable (teacher_id, timeslot_id, reason) VALUES (:teacherId, :timeslotId, :reason)";
        var params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("timeslotId", timeslotId)
                .addValue("reason", reason);
        var kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh);
        return kh.getKey().longValue();
    }

    /** Lưu hàng loạt ca bận của giáo viên */
    public void batchSave(Long teacherId, List<Long> timeslotIds) {
        if (timeslotIds == null || timeslotIds.isEmpty()) return;
        
        String sql = "INSERT INTO teacher_unavailable (teacher_id, timeslot_id, reason) VALUES (:teacherId, :timeslotId, 'Đăng ký lịch bận')";
        MapSqlParameterSource[] batchParams = new MapSqlParameterSource[timeslotIds.size()];
        for (int i = 0; i < timeslotIds.size(); i++) {
            batchParams[i] = new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("timeslotId", timeslotIds.get(i));
        }
        jdbc.batchUpdate(sql, batchParams);
    }
}
