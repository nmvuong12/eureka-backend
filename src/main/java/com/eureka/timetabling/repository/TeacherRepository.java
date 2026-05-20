package com.eureka.timetabling.repository;

import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.domain.TeacherType;
import com.eureka.timetabling.domain.WorkingStatus;
import com.eureka.timetabling.domain.Gender;
import com.eureka.timetabling.solver.Timetable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý truy vấn dữ liệu Giáo viên bằng NamedParameterJdbcTemplate.
 * Thực hiện các câu lệnh SQL tối ưu, ánh xạ dữ liệu thủ công và xây dựng câu truy vấn động chuẩn mực.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * RowMapper ánh xạ bản ghi từ ResultSet sang thực thể Teacher
     */
    private final RowMapper<Teacher> teacherMapper = (rs, rowNum) -> Teacher.builder()
            .id(rs.getLong("id"))
            .teacherCode(rs.getString("teacher_code"))
            .teacherType(TeacherType.valueOf(rs.getString("teacher_type")))
            .fullName(rs.getString("full_name"))
            .dateOfBirth(rs.getDate("date_of_birth") != null 
                    ? rs.getDate("date_of_birth").toLocalDate() : null)
            .gender(Gender.valueOf(rs.getString("gender")))
            .address(rs.getString("address"))
            .email(rs.getString("email"))
            .phone(rs.getString("phone"))
            .skills(rs.getString("skills"))
            .certificateFile(rs.getString("certificate_file"))
            .profileFile(rs.getString("profile_file"))
            .workingStatus(WorkingStatus.valueOf(rs.getString("working_status")))
            .deleted(rs.getBoolean("is_deleted"))
            .createdDate(rs.getTimestamp("created_date") != null 
                    ? rs.getTimestamp("created_date").toLocalDateTime() : null)
            .modifiedDate(rs.getTimestamp("modified_date") != null 
                    ? rs.getTimestamp("modified_date").toLocalDateTime() : null)
            .build();

    /**
     * Tìm kiếm động (Dynamic Query Builder - Thay thế cho JPA Specification)
     * Hỗ trợ tìm kiếm, phân trang và sắp xếp động.
     */
    public List<Teacher> search(String teacherCode, String fullName, String skill, 
                                 String teacherType, String workingStatus, 
                                 int page, int size, String sortBy, String sortDir) {
        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT t.id, t.teacher_code, t.teacher_type, t.full_name, t.date_of_birth,
                                t.gender, t.address, t.email, t.phone, t.skills, t.certificate_file,
                                t.profile_file, t.working_status, t.is_deleted, t.created_date, t.modified_date
                FROM teacher t
                """);

        // Nếu có tìm kiếm theo kỹ năng trong bảng liên kết teacher_skill cũ, join thêm
        if (skill != null && !skill.isBlank()) {
            sql.append(" LEFT JOIN teacher_skill ts ON t.id = ts.teacher_id ");
        }

        sql.append(" WHERE t.is_deleted = 0 ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (teacherCode != null && !teacherCode.isBlank()) {
            sql.append(" AND LOWER(t.teacher_code) LIKE LOWER(:teacherCode) ");
            params.addValue("teacherCode", "%" + teacherCode.trim() + "%");
        }
        if (fullName != null && !fullName.isBlank()) {
            sql.append(" AND LOWER(t.full_name) LIKE LOWER(:fullName) ");
            params.addValue("fullName", "%" + fullName.trim() + "%");
        }
        if (skill != null && !skill.isBlank()) {
            // Tìm kiếm cả trên trường skills (chuỗi dài) và trong bảng teacher_skill liên kết
            sql.append(" AND (LOWER(t.skills) LIKE LOWER(:skill) OR LOWER(ts.skill_code) LIKE LOWER(:skill)) ");
            params.addValue("skill", "%" + skill.trim() + "%");
        }
        if (teacherType != null && !teacherType.isBlank()) {
            sql.append(" AND t.teacher_type = :teacherType ");
            params.addValue("teacherType", teacherType);
        }
        if (workingStatus != null && !workingStatus.isBlank()) {
            sql.append(" AND t.working_status = :workingStatus ");
            params.addValue("workingStatus", workingStatus);
        }

        // Xử lý Sort động an toàn để tránh lỗi SQL Injection
        List<String> allowedSortFields = List.of("id", "teacher_code", "full_name", "date_of_birth", "working_status", "created_date");
        String finalSortBy = allowedSortFields.contains(sortBy) ? "t." + sortBy : "t.teacher_code";
        String finalSortDir = "DESC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC";
        
        sql.append(" ORDER BY ").append(finalSortBy).append(" ").append(finalSortDir);
        
        // Phân trang
        sql.append(" LIMIT :limit OFFSET :offset ");
        params.addValue("limit", size);
        params.addValue("offset", (page - 1) * size);

        return jdbc.query(sql.toString(), params, teacherMapper);
    }

    /**
     * Đếm tổng số bản ghi khớp bộ lọc tìm kiếm động (Để phục vụ phân trang)
     */
    public long countSearch(String teacherCode, String fullName, String skill, 
                            String teacherType, String workingStatus) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT t.id)
                FROM teacher t
                """);

        if (skill != null && !skill.isBlank()) {
            sql.append(" LEFT JOIN teacher_skill ts ON t.id = ts.teacher_id ");
        }

        sql.append(" WHERE t.is_deleted = 0 ");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (teacherCode != null && !teacherCode.isBlank()) {
            sql.append(" AND LOWER(t.teacher_code) LIKE LOWER(:teacherCode) ");
            params.addValue("teacherCode", "%" + teacherCode.trim() + "%");
        }
        if (fullName != null && !fullName.isBlank()) {
            sql.append(" AND LOWER(t.full_name) LIKE LOWER(:fullName) ");
            params.addValue("fullName", "%" + fullName.trim() + "%");
        }
        if (skill != null && !skill.isBlank()) {
            sql.append(" AND (LOWER(t.skills) LIKE LOWER(:skill) OR LOWER(ts.skill_code) LIKE LOWER(:skill)) ");
            params.addValue("skill", "%" + skill.trim() + "%");
        }
        if (teacherType != null && !teacherType.isBlank()) {
            sql.append(" AND t.teacher_type = :teacherType ");
            params.addValue("teacherType", teacherType);
        }
        if (workingStatus != null && !workingStatus.isBlank()) {
            sql.append(" AND t.working_status = :workingStatus ");
            params.addValue("workingStatus", workingStatus);
        }

        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Lấy toàn bộ giáo viên chưa bị xóa theo bộ lọc trạng thái (nếu có)
     */
    public List<Teacher> findAll(String status) {
        String sql = """
                SELECT id, teacher_code, teacher_type, full_name, date_of_birth, gender, address,
                       email, phone, skills, certificate_file, profile_file, working_status,
                       is_deleted, created_date, modified_date
                FROM teacher
                WHERE is_deleted = 0 AND (:status IS NULL OR working_status = :status)
                ORDER BY full_name
                """;
        return jdbc.query(sql, new MapSqlParameterSource("status", status), teacherMapper);
    }

    /**
     * Lấy danh sách ID giáo viên hoạt động (dùng cho bộ giải thuật Timefold Solver)
     */
    public List<Long> findAllActiveIds() {
        String sql = "SELECT id FROM teacher WHERE working_status = 'ACTIVE' AND is_deleted = 0";
        return jdbc.queryForList(sql, new MapSqlParameterSource(), Long.class);
    }

    /**
     * Tìm kiếm giáo viên theo ID
     */
    public Optional<Teacher> findById(Long id) {
        String sql = """
                SELECT id, teacher_code, teacher_type, full_name, date_of_birth, gender, address,
                       email, phone, skills, certificate_file, profile_file, working_status,
                       is_deleted, created_date, modified_date
                FROM teacher
                WHERE id = :id AND is_deleted = 0
                """;
        List<Teacher> list = jdbc.query(sql, new MapSqlParameterSource("id", id), teacherMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Tìm giáo viên theo Email (không phân biệt hoa thường)
     */
    public Optional<Teacher> findByEmail(String email) {
        String sql = """
                SELECT id, teacher_code, teacher_type, full_name, date_of_birth, gender, address,
                       email, phone, skills, certificate_file, profile_file, working_status,
                       is_deleted, created_date, modified_date
                FROM teacher
                WHERE LOWER(email) = LOWER(:email) AND is_deleted = 0
                """;
        List<Teacher> list = jdbc.query(sql, new MapSqlParameterSource("email", email), teacherMapper);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Lấy mã số giáo viên (teacher_code) lớn nhất hiện có trong DB
     */
    public Optional<String> findMaxTeacherCode() {
        String sql = "SELECT MAX(teacher_code) FROM teacher";
        String maxCode = jdbc.queryForObject(sql, new MapSqlParameterSource(), String.class);
        return Optional.ofNullable(maxCode);
    }

    /**
     * Lưu thông tin Giáo viên mới vào cơ sở dữ liệu
     */
    public Long save(Teacher teacher) {
        String sql = """
                INSERT INTO teacher (teacher_code, teacher_type, full_name, date_of_birth, gender,
                                     address, email, phone, skills, certificate_file, profile_file,
                                     working_status, is_deleted, created_date, modified_date)
                VALUES (:teacherCode, :teacherType, :fullName, :dateOfBirth, :gender,
                        :address, :email, :phone, :skills, :certificateFile, :profileFile,
                        :workingStatus, 0, NOW(), NOW())
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("teacherCode", teacher.getTeacherCode())
                .addValue("teacherType", teacher.getTeacherType().name())
                .addValue("fullName", teacher.getFullName())
                .addValue("dateOfBirth", teacher.getDateOfBirth())
                .addValue("gender", teacher.getGender().name())
                .addValue("address", teacher.getAddress())
                .addValue("email", teacher.getEmail())
                .addValue("phone", teacher.getPhone())
                .addValue("skills", teacher.getSkills())
                .addValue("certificateFile", teacher.getCertificateFile())
                .addValue("profileFile", teacher.getProfileFile())
                .addValue("workingStatus", teacher.getWorkingStatus().name());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder);
        return keyHolder.getKey().longValue();
    }

    /**
     * Cập nhật thông tin Giáo viên hiện có
     */
    public int update(Teacher teacher) {
        String sql = """
                UPDATE teacher
                SET teacher_type = :teacherType,
                    full_name = :fullName,
                    date_of_birth = :dateOfBirth,
                    gender = :gender,
                    address = :address,
                    email = :email,
                    phone = :phone,
                    skills = :skills,
                    certificate_file = :certificateFile,
                    profile_file = :profileFile,
                    working_status = :workingStatus,
                    modified_date = NOW()
                WHERE id = :id AND is_deleted = 0
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", teacher.getId())
                .addValue("teacherType", teacher.getTeacherType().name())
                .addValue("fullName", teacher.getFullName())
                .addValue("dateOfBirth", teacher.getDateOfBirth())
                .addValue("gender", teacher.getGender().name())
                .addValue("address", teacher.getAddress())
                .addValue("email", teacher.getEmail())
                .addValue("phone", teacher.getPhone())
                .addValue("skills", teacher.getSkills())
                .addValue("certificateFile", teacher.getCertificateFile())
                .addValue("profileFile", teacher.getProfileFile())
                .addValue("workingStatus", teacher.getWorkingStatus().name());

        return jdbc.update(sql, params);
    }

    /**
     * Thực hiện xóa mềm Giáo viên bằng cách set is_deleted = 1
     */
    public int deleteById(Long id) {
        String sql = "UPDATE teacher SET is_deleted = 1, modified_date = NOW() WHERE id = :id";
        return jdbc.update(sql, new MapSqlParameterSource("id", id));
    }

    /**
     * Kiểm tra xem Email đã tồn tại trong các giáo viên chưa xóa khác
     */
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE email = :email AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("email", email), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Kiểm tra xem Email đã tồn tại ở bản ghi khác (không phải chính ID đó)
     */
    public boolean existsByEmailAndIdNot(String email, Long id) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE email = :email AND id != :id AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("email", email).addValue("id", id), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Kiểm tra xem Số điện thoại đã tồn tại trong các giáo viên chưa xóa khác
     */
    public boolean existsByPhone(String phone) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE phone = :phone AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("phone", phone), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Kiểm tra xem Số điện thoại đã tồn tại ở bản ghi khác (không phải chính ID đó)
     */
    public boolean existsByPhoneAndIdNot(String phone, Long id) {
        String sql = "SELECT COUNT(*) FROM teacher WHERE phone = :phone AND id != :id AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql,
                new MapSqlParameterSource("phone", phone).addValue("id", id), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Kiểm tra xem giáo viên có lịch dạy nào đang có hiệu lực trong lesson_assignment hay không
     */
    public boolean hasAssignments(Long teacherId) {
        String sql = "SELECT COUNT(*) FROM lesson_assignment WHERE teacher_id = :teacherId AND is_deleted = 0";
        Integer count = jdbc.queryForObject(sql, new MapSqlParameterSource("teacherId", teacherId), Integer.class);
        return count != null && count > 0;
    }

    /**
     * Lấy kỹ năng của giáo viên theo ID giáo viên (Cho mục đích tương thích ngược)
     */
    public List<String> findSkillsByTeacherId(Long teacherId) {
        String sql = "SELECT skill_code FROM teacher_skill WHERE teacher_id = :teacherId ORDER BY skill_code";
        return jdbc.queryForList(sql, new MapSqlParameterSource("teacherId", teacherId), String.class);
    }

    /**
     * Cập nhật các kỹ năng giáo viên trong bảng liên kết (Để đảm bảo tương thích hệ thống)
     */
    public void replaceSkills(Long teacherId, List<String> skills) {
        jdbc.update("DELETE FROM teacher_skill WHERE teacher_id = :teacherId",
                new MapSqlParameterSource("teacherId", teacherId));
        if (skills != null && !skills.isEmpty()) {
            String insertSql = "INSERT IGNORE INTO teacher_skill (teacher_id, skill_code) VALUES (:teacherId, :skillCode)";
            var batchParams = skills.stream()
                    .map(s -> new MapSqlParameterSource("teacherId", teacherId).addValue("skillCode", s))
                    .toArray(MapSqlParameterSource[]::new);
            jdbc.batchUpdate(insertSql, batchParams);
        }
    }

    /**
     * Trả về toàn bộ dữ liệu cấu kiện kỹ năng phục vụ Solver
     */
    public List<Timetable.TeacherSkillFact> findAllSkillFacts() {
        String sql = "SELECT teacher_id, skill_code FROM teacher_skill";
        return jdbc.query(sql, (rs, row) ->
                new Timetable.TeacherSkillFact(rs.getLong("teacher_id"), rs.getString("skill_code")));
    }

    /**
     * Tìm giáo viên thay thế phù hợp (Có kỹ năng phù hợp, trống lịch dạy và không bận vào ca học cụ thể)
     */
    public List<Teacher> findSuitableSubstitutes(String skillCode, Long timeslotId, Long excludeTeacherId) {
        String sql = """
                SELECT DISTINCT t.id, t.teacher_code, t.teacher_type, t.full_name, t.date_of_birth,
                                t.gender, t.address, t.email, t.phone, t.skills, t.certificate_file,
                                t.profile_file, t.working_status, t.is_deleted, t.created_date, t.modified_date
                FROM teacher t
                INNER JOIN teacher_skill ts ON t.id = ts.teacher_id AND ts.skill_code = :skillCode
                LEFT JOIN teacher_unavailable tu ON t.id = tu.teacher_id AND tu.timeslot_id = :timeslotId
                LEFT JOIN lesson_assignment la ON t.id = la.teacher_id AND la.timeslot_id = :timeslotId AND la.is_deleted = 0
                WHERE t.working_status = 'ACTIVE' AND t.is_deleted = 0
                  AND t.id != :excludeTeacherId
                  AND tu.id IS NULL
                  AND la.id IS NULL
                ORDER BY t.full_name
                """;
        var params = new MapSqlParameterSource()
                .addValue("skillCode", skillCode)
                .addValue("timeslotId", timeslotId)
                .addValue("excludeTeacherId", excludeTeacherId);
        return jdbc.query(sql, params, teacherMapper);
    }
}
