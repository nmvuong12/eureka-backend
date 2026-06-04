package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.domain.LeaveRequest;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.Room;
import com.eureka.timetabling.domain.SubstituteOffer;
import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.dto.request.LeaveRequestDto;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.RoomRepository;
import com.eureka.timetabling.notification.EmailNotificationService;
import com.eureka.timetabling.notification.NotificationSseService;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.LeaveRequestRepository;
import com.eureka.timetabling.repository.SubstituteOfferRepository;
import com.eureka.timetabling.repository.TeacherRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Service quản lý đơn xin nghỉ và quy trình xử lý sau phê duyệt */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final EmailNotificationService notificationService;
    private final NotificationSseService sseService;
    private final SubstituteOfferRepository substituteOfferRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RoomRepository roomRepository;


    @Transactional(readOnly = true)
    public List<LeaveRequest> findAll(String status) {
        return leaveRequestRepository.findAll(status);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> findByTeacherId(Long teacherId) {
        return leaveRequestRepository.findByTeacherId(teacherId);
    }

    /** Tìm các buổi học bị ảnh hưởng bởi danh sách ngày nghỉ chi tiết và buổi nghỉ tương ứng */
    @Transactional(readOnly = true)
    public List<Lesson> getAffectedLessons(Long teacherId, List<LeaveRequestDto.DayConfigDto> dayConfigs) {
        if (dayConfigs == null || dayConfigs.isEmpty()) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id, la.session_date,
                       la.original_session_date, la.original_timeslot_id, la.original_room_id, la.reschedule_reason, la.leave_request_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned,
                       c.schedule_pattern_id,
                       c.teacher_id AS class_teacher_id,
                       COALESCE(c.class_code, c.name) AS class_code
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                INNER JOIN timeslot ts ON la.timeslot_id = ts.id
                WHERE la.teacher_id = :teacherId
                  AND c.is_deleted = 0 AND l.is_deleted = 0
                """);

        var params = new MapSqlParameterSource().addValue("teacherId", teacherId);

        sql.append(" AND (");
        for (int i = 0; i < dayConfigs.size(); i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            LeaveRequestDto.DayConfigDto conf = dayConfigs.get(i);
            String dateParam = "date_" + i;
            params.addValue(dateParam, conf.getDate());
            sql.append("(la.session_date = :").append(dateParam);

            if ("MORNING".equalsIgnoreCase(conf.getSessionType())) {
                sql.append(" AND ts.start_time < '13:00:00'");
            } else if ("AFTERNOON".equalsIgnoreCase(conf.getSessionType())) {
                sql.append(" AND ts.start_time >= '13:00:00'");
            }
            sql.append(")");
        }
        sql.append(")");
        sql.append(" ORDER BY la.session_date, ts.start_time");

        RowMapper<Lesson> affectedMapper = (rs, row) -> Lesson.builder()
                .id(rs.getLong("id"))
                .classId(rs.getLong("class_id"))
                .lessonIndex(rs.getInt("lesson_index"))
                .requiredSkill(rs.getString("required_skill"))
                .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
                .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
                .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
                .pinned(rs.getBoolean("is_pinned"))
                .schedulePatternId(rs.getObject("schedule_pattern_id") != null ? rs.getLong("schedule_pattern_id") : null)
                .classTeacherId(rs.getObject("class_teacher_id") != null ? rs.getLong("class_teacher_id") : null)
                .sessionDate(rs.getDate("session_date") != null ? rs.getDate("session_date").toLocalDate() : null)
                .originalSessionDate(rs.getDate("original_session_date") != null ? rs.getDate("original_session_date").toLocalDate() : null)
                .originalTimeslotId(rs.getObject("original_timeslot_id") != null ? rs.getLong("original_timeslot_id") : null)
                .originalRoomId(rs.getObject("original_room_id") != null ? rs.getLong("original_room_id") : null)
                .rescheduleReason(rs.getString("reschedule_reason"))
                .leaveRequestId(rs.getObject("leave_request_id") != null ? rs.getLong("leave_request_id") : null)
                .classCode(rs.getString("class_code"))
                .build();


        return jdbc.query(sql.toString(), params, affectedMapper);
    }

    /** Tìm các buổi học bị ảnh hưởng bởi khoảng ngày nghỉ và ca học (Tương thích ngược) */
    @Transactional(readOnly = true)
    public List<Lesson> getAffectedLessons(Long teacherId, LocalDate fromDate, LocalDate toDate, String sessionType) {
        List<LeaveRequestDto.DayConfigDto> dayConfigs = new java.util.ArrayList<>();
        LocalDate current = fromDate;
        while (current != null && toDate != null && !current.isAfter(toDate)) {
            LeaveRequestDto.DayConfigDto conf = new LeaveRequestDto.DayConfigDto();
            conf.setDate(current);
            conf.setSessionType(sessionType);
            dayConfigs.add(conf);
            current = current.plusDays(1);
        }
        return getAffectedLessons(teacherId, dayConfigs);
    }

    /** Tìm các buổi học bị ảnh hưởng bởi đơn xin nghỉ (dùng leave_request_id để giữ nguyên danh sách sau khi đã duyệt) */
    @Transactional(readOnly = true)
    public List<Lesson> getAffectedLessonsByRequest(Long leaveRequestId, Long teacherId, LocalDate fromDate, LocalDate toDate, String sessionType) {
        return getAffectedLessonsByRequest(leaveRequestId, teacherId, fromDate, toDate, sessionType, null);
    }

    /** Kiểm tra tính khả thi của ca dạy bù dự kiến - trả về chuỗi thông báo kết quả (tương thích ngược) */
    @Transactional(readOnly = true)
    public String assessFeasibility(LocalDate date, Long timeslotId, Long roomId, Long classId, Long teacherId) {
        return assessFeasibility(date, timeslotId, roomId, classId, teacherId, null);
    }

    /** Kiểm tra tính khả thi của ca dạy bù dự kiến - trả về chuỗi thông báo kết quả (có hỗ trợ loại trừ buổi học hiện tại) */
    @Transactional(readOnly = true)
    public String assessFeasibility(LocalDate date, Long timeslotId, Long roomId, Long classId, Long teacherId, Long excludeLessonId) {
        if (date == null || timeslotId == null) {
            return "Thiếu thông tin ngày/ca";
        }

        // 1. Kiểm tra lớp học bị trùng lịch học khác
        if (classId != null) {
            String classSql = """
                    SELECT COUNT(*) FROM lesson_assignment la
                    INNER JOIN lesson l ON la.lesson_id = l.id
                    WHERE l.class_id = :classId AND la.session_date = :date AND la.timeslot_id = :timeslotId AND la.is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                classSql += " AND la.lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("classId", classId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(classSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                return "Trùng lịch học của lớp";
            }
        }

        // 2. Kiểm tra giáo viên trống lịch dạy
        if (teacherId != null) {
            String teacherSql = """
                    SELECT COUNT(*) FROM lesson_assignment
                    WHERE teacher_id = :teacherId AND session_date = :date AND timeslot_id = :timeslotId AND is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                teacherSql += " AND lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(teacherSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                return "Giáo viên trùng lịch dạy";
            }

            // Kiểm tra bận cố định
            String unavailSql = """
                    SELECT COUNT(*) FROM teacher_unavailable
                    WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId
                    """;
            Integer unavail = jdbc.queryForObject(unavailSql, params, Integer.class);
            if (unavail != null && unavail > 0) {
                return "Giáo viên bận cố định";
            }

            // Kiểm tra nghỉ phép
            String leaveSql = """
                    SELECT COUNT(*) FROM leave_request
                    WHERE teacher_id = :teacherId AND status = 'APPROVED' AND :date BETWEEN from_date AND to_date AND is_deleted = 0
                    """;
            Integer isOnLeave = jdbc.queryForObject(leaveSql, params, Integer.class);
            if (isOnLeave != null && isOnLeave > 0) {
                return "Giáo viên bận nghỉ phép";
            }
        }

        // 3. Kiểm tra phòng học trống
        if (roomId != null) {
            String roomSql = """
                    SELECT COUNT(*) FROM lesson_assignment
                    WHERE room_id = :roomId AND session_date = :date AND timeslot_id = :timeslotId AND is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                roomSql += " AND lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("roomId", roomId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(roomSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                return "Phòng học bị trùng lịch";
            }
        }

        return "KHẢ THI";
    }

    /** Tìm các buổi học bị ảnh hưởng bởi đơn xin nghỉ có hỗ trợ tính toán feasibilityNote động theo phòng học được chọn */
    @Transactional(readOnly = true)
    public List<Lesson> getAffectedLessonsByRequest(Long leaveRequestId, Long teacherId, LocalDate fromDate, LocalDate toDate, String sessionType, Long makeupRoomId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId).orElse(null);
        List<LeaveRequestDto.DayConfigDto> dayConfigs = null;
        if (lr != null && lr.getDayConfigs() != null && !lr.getDayConfigs().isBlank()) {
            try {
                dayConfigs = objectMapper.readValue(lr.getDayConfigs(), new com.fasterxml.jackson.core.type.TypeReference<List<LeaveRequestDto.DayConfigDto>>() {});
            } catch (Exception e) {
                log.error("Lỗi parse dayConfigs từ đơn xin nghỉ #{}: {}", leaveRequestId, e.getMessage());
            }
        }

        StringBuilder sql = new StringBuilder("""
                SELECT l.id, l.class_id, l.lesson_index, l.required_skill,
                       la.teacher_id, la.room_id, la.timeslot_id, la.session_date,
                       la.original_session_date, la.original_timeslot_id, la.original_room_id, la.reschedule_reason, la.leave_request_id,
                       COALESCE(la.is_pinned, 0) AS is_pinned,
                       c.schedule_pattern_id,
                       c.teacher_id AS class_teacher_id,
                       COALESCE(c.class_code, c.name) AS class_code,
                       lrl.makeup_option,
                       lrl.makeup_date,
                       lrl.makeup_timeslot_id,
                       t.full_name AS teacher_name
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN teacher t ON la.teacher_id = t.id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                LEFT JOIN leave_request_lesson lrl ON lrl.leave_request_id = :leaveRequestId AND lrl.lesson_id = l.id
                WHERE (la.leave_request_id = :leaveRequestId OR lrl.lesson_id IS NOT NULL
                """);

        var params = new MapSqlParameterSource()
                .addValue("leaveRequestId", leaveRequestId)
                .addValue("teacherId", teacherId);

        // Thêm điều kiện fallback thời gian
        sql.append(" OR (la.teacher_id = :teacherId");
        if (dayConfigs != null && !dayConfigs.isEmpty()) {
            sql.append(" AND (");
            for (int i = 0; i < dayConfigs.size(); i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                LeaveRequestDto.DayConfigDto conf = dayConfigs.get(i);
                String dateParam = "date_" + i;
                params.addValue(dateParam, conf.getDate());
                sql.append("(la.session_date = :").append(dateParam);
                if ("MORNING".equalsIgnoreCase(conf.getSessionType())) {
                    sql.append(" AND ts.start_time < '13:00:00'");
                } else if ("AFTERNOON".equalsIgnoreCase(conf.getSessionType())) {
                    sql.append(" AND ts.start_time >= '13:00:00'");
                }
                sql.append(")");
            }
            sql.append(")");
        } else {
            params.addValue("fromDate", fromDate)
                  .addValue("toDate", toDate);
            sql.append(" AND la.session_date BETWEEN :fromDate AND :toDate");
            if ("MORNING".equalsIgnoreCase(sessionType)) {
                sql.append(" AND ts.start_time < '13:00:00'");
            } else if ("AFTERNOON".equalsIgnoreCase(sessionType)) {
                sql.append(" AND ts.start_time >= '13:00:00'");
            }
        }
        sql.append("))"); // Kết thúc điều kiện OR fallback và teacher_id

        sql.append(" AND c.is_deleted = 0 AND l.is_deleted = 0");
        sql.append(" ORDER BY la.session_date, la.timeslot_id");

        final java.util.Set<String> classSlots = new java.util.HashSet<>();
        final java.util.Set<String> teacherSlots = new java.util.HashSet<>();
        final java.util.Set<String> roomSlots = new java.util.HashSet<>();

        RowMapper<Lesson> affectedMapper = (rs, row) -> {
            Lesson lesson = Lesson.builder()
                    .id(rs.getLong("id"))
                    .classId(rs.getLong("class_id"))
                    .lessonIndex(rs.getInt("lesson_index"))
                    .requiredSkill(rs.getString("required_skill"))
                    .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
                    .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
                    .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
                    .pinned(rs.getBoolean("is_pinned"))
                    .schedulePatternId(rs.getObject("schedule_pattern_id") != null ? rs.getLong("schedule_pattern_id") : null)
                    .classTeacherId(rs.getObject("class_teacher_id") != null ? rs.getLong("class_teacher_id") : null)
                    .sessionDate(rs.getDate("session_date") != null ? rs.getDate("session_date").toLocalDate() : null)
                    .originalSessionDate(rs.getDate("original_session_date") != null ? rs.getDate("original_session_date").toLocalDate() : null)
                    .originalTimeslotId(rs.getObject("original_timeslot_id") != null ? rs.getLong("original_timeslot_id") : null)
                    .originalRoomId(rs.getObject("original_room_id") != null ? rs.getLong("original_room_id") : null)
                    .rescheduleReason(rs.getString("reschedule_reason"))
                    .leaveRequestId(rs.getObject("leave_request_id") != null ? rs.getLong("leave_request_id") : null)
                    .classCode(rs.getString("class_code"))
                    .makeupOption(rs.getString("makeup_option") != null ? rs.getString("makeup_option") : (lr != null ? lr.getMakeupOption() : null))
                    .makeupDate(rs.getDate("makeup_date") != null ? rs.getDate("makeup_date").toLocalDate() : (lr != null ? lr.getMakeupDate() : null))
                    .makeupTimeslotId(rs.getObject("makeup_timeslot_id") != null ? rs.getLong("makeup_timeslot_id") : (lr != null ? lr.getMakeupTimeslotId() : null))
                    .teacherName(rs.getString("teacher_name"))
                    .build();

            // Tính toán feasibilityNote nếu là ca dạy bù
            if ("MAKEUP".equals(lesson.getMakeupOption())) {
                LocalDate mDate = lesson.getMakeupDate();
                Long mTimeslotId = lesson.getMakeupTimeslotId();
                Long targetRoomId = makeupRoomId != null ? makeupRoomId : lesson.getRoomId();
                
                if (mDate != null && mTimeslotId != null) {
                    String slotKey = mDate + "_" + mTimeslotId;
                    if (!classSlots.add(lesson.getClassId() + "_" + slotKey)) {
                        lesson.setFeasibilityNote("Trùng lịch học của lớp (trùng ca dạy bù khác trong đơn)");
                    } else if (!teacherSlots.add(teacherId + "_" + slotKey)) {
                        lesson.setFeasibilityNote("Giáo viên trùng lịch dạy (trùng ca dạy bù khác trong đơn)");
                    } else if (targetRoomId != null && !roomSlots.add(targetRoomId + "_" + slotKey)) {
                        lesson.setFeasibilityNote("Phòng học bị trùng lịch (trùng ca dạy bù khác trong đơn)");
                    } else {
                        String note = assessFeasibility(mDate, mTimeslotId, targetRoomId, lesson.getClassId(), teacherId, lesson.getId());
                        lesson.setFeasibilityNote(note);
                    }
                } else {
                    lesson.setFeasibilityNote("Thiếu thông tin ngày/ca");
                }
            }

            return lesson;
        };

        return jdbc.query(sql.toString(), params, affectedMapper);
    }

    @Transactional
    public LeaveRequest create(Long teacherId, LeaveRequestDto dto) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", teacherId));

        if (dto.getToDate().isBefore(dto.getFromDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Chuẩn hóa dayConfigs nếu chưa có (tương thích ngược)
        if (dto.getDayConfigs() == null || dto.getDayConfigs().isEmpty()) {
            List<LeaveRequestDto.DayConfigDto> configs = new java.util.ArrayList<>();
            LocalDate current = dto.getFromDate();
            while (current != null && dto.getToDate() != null && !current.isAfter(dto.getToDate())) {
                LeaveRequestDto.DayConfigDto conf = new LeaveRequestDto.DayConfigDto();
                conf.setDate(current);
                conf.setSessionType(dto.getSessionType() != null ? dto.getSessionType() : "ALL_DAY");
                configs.add(conf);
                current = current.plusDays(1);
            }
            dto.setDayConfigs(configs);
        }

        String globalSessionType = dto.getSessionType();
        if (globalSessionType == null || globalSessionType.isBlank()) {
            if (dto.getDayConfigs() != null && !dto.getDayConfigs().isEmpty()) {
                globalSessionType = dto.getDayConfigs().get(0).getSessionType();
            } else {
                globalSessionType = "ALL_DAY";
            }
        }

        String dayConfigsJson = null;
        try {
            dayConfigsJson = objectMapper.writeValueAsString(dto.getDayConfigs());
        } catch (Exception e) {
            log.error("Lỗi serialize dayConfigs: {}", e.getMessage());
        }

        // Kiểm tra xem giáo viên có tiết học nào trong khoảng thời gian này không
        List<Lesson> affected = getAffectedLessons(teacherId, dto.getDayConfigs());

        // Tự động tính toán các trường global đại diện từ các tùy chọn chi tiết để tương thích ngược hiển thị bảng
        String globalMakeupOption = dto.getMakeupOption();
        LocalDate globalMakeupDate = dto.getMakeupDate();
        Long globalMakeupTimeslotId = dto.getMakeupTimeslotId();

        if (dto.getLessonOptions() != null && !dto.getLessonOptions().isEmpty()) {
            boolean hasMakeup = dto.getLessonOptions().stream()
                    .anyMatch(opt -> "MAKEUP".equals(opt.getMakeupOption()));
            if (hasMakeup) {
                globalMakeupOption = "MAKEUP";
                var firstMakeup = dto.getLessonOptions().stream()
                        .filter(opt -> "MAKEUP".equals(opt.getMakeupOption()))
                        .findFirst().orElse(null);
                if (firstMakeup != null) {
                    globalMakeupDate = firstMakeup.getMakeupDate();
                    globalMakeupTimeslotId = firstMakeup.getMakeupTimeslotId();
                }
            } else {
                globalMakeupOption = "NO_MAKEUP";
                globalMakeupDate = null;
                globalMakeupTimeslotId = null;
            }
        }

        LeaveRequest lr = LeaveRequest.builder()
                .teacherId(teacherId)
                .fromDate(dto.getFromDate())
                .toDate(dto.getToDate())
                .reason(dto.getReason())
                .status("PENDING")
                .sessionType(globalSessionType)
                .dayConfigs(dayConfigsJson)
                .makeupOption(globalMakeupOption)
                .makeupDate(globalMakeupDate)
                .makeupTimeslotId(globalMakeupTimeslotId)
                .build();
        Long id = leaveRequestRepository.save(lr);
        lr.setId(id);

        // Lưu thông tin phương án xử lý chi tiết cho từng buổi học
        if (dto.getLessonOptions() != null && !dto.getLessonOptions().isEmpty()) {
            for (LeaveRequestDto.LessonOptionDto opt : dto.getLessonOptions()) {
                String insertOptionSql = """
                        INSERT INTO leave_request_lesson (leave_request_id, lesson_id, makeup_option, makeup_date, makeup_timeslot_id)
                        VALUES (:leaveRequestId, :lessonId, :makeupOption, :makeupDate, :makeupTimeslotId)
                        """;
                jdbc.update(insertOptionSql, new MapSqlParameterSource()
                        .addValue("leaveRequestId", id)
                        .addValue("lessonId", opt.getLessonId())
                        .addValue("makeupOption", opt.getMakeupOption())
                        .addValue("makeupDate", opt.getMakeupDate() != null ? java.sql.Date.valueOf(opt.getMakeupDate()) : null)
                        .addValue("makeupTimeslotId", opt.getMakeupTimeslotId()));
            }
        } else {
            // Fallback: Tự động tạo phương án dựa trên thông tin chung
            for (Lesson lesson : affected) {
                String insertOptionSql = """
                        INSERT INTO leave_request_lesson (leave_request_id, lesson_id, makeup_option, makeup_date, makeup_timeslot_id)
                        VALUES (:leaveRequestId, :lessonId, :makeupOption, :makeupDate, :makeupTimeslotId)
                        """;
                jdbc.update(insertOptionSql, new MapSqlParameterSource()
                        .addValue("leaveRequestId", id)
                        .addValue("lessonId", lesson.getId())
                        .addValue("makeupOption", dto.getMakeupOption() != null ? dto.getMakeupOption() : "NO_MAKEUP")
                        .addValue("makeupDate", dto.getMakeupDate() != null ? java.sql.Date.valueOf(dto.getMakeupDate()) : null)
                        .addValue("makeupTimeslotId", dto.getMakeupTimeslotId()));
            }
        }
        
        log.info("Giáo viên {} đã nộp đơn xin nghỉ từ {} đến {}, tiết bị ảnh hưởng: {}", 
                teacherId, dto.getFromDate(), dto.getToDate(), affected.size());

        // Gửi thông báo đẩy thời gian thực (SSE) cho Admin và Staff
        try {
            String message = String.format("Giáo viên %s đã gửi đơn xin nghỉ phép từ %s đến %s (Số buổi dạy ảnh hưởng: %d)", 
                    teacher.getFullName(), lr.getFromDate(), lr.getToDate(), affected.size());
            sseService.saveAndBroadcast(List.of("ADMIN", "STAFF"), message);
        } catch (Exception e) {
            log.error("Lỗi khi gửi thông báo SSE: {}", e.getMessage());
        }

        return lr;
    }

    @Transactional
    public LeaveRequest update(Long id, Long teacherId, LeaveRequestDto dto) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));

        // Kiểm tra quyền sở hữu
        if (!lr.getTeacherId().equals(teacherId)) {
            throw new BusinessException("Bạn không có quyền chỉnh sửa đơn xin nghỉ của giáo viên khác");
        }

        // Chỉ được phép chỉnh sửa đơn ở trạng thái PENDING
        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("Chỉ có thể chỉnh sửa đơn xin nghỉ đang ở trạng thái chờ duyệt");
        }

        if (dto.getToDate().isBefore(dto.getFromDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        // Chuẩn hóa dayConfigs nếu chưa có
        if (dto.getDayConfigs() == null || dto.getDayConfigs().isEmpty()) {
            List<LeaveRequestDto.DayConfigDto> configs = new java.util.ArrayList<>();
            LocalDate current = dto.getFromDate();
            while (current != null && dto.getToDate() != null && !current.isAfter(dto.getToDate())) {
                LeaveRequestDto.DayConfigDto conf = new LeaveRequestDto.DayConfigDto();
                conf.setDate(current);
                conf.setSessionType(dto.getSessionType() != null ? dto.getSessionType() : "ALL_DAY");
                configs.add(conf);
                current = current.plusDays(1);
            }
            dto.setDayConfigs(configs);
        }

        String globalSessionType = dto.getSessionType();
        if (globalSessionType == null || globalSessionType.isBlank()) {
            if (!dto.getDayConfigs().isEmpty()) {
                globalSessionType = dto.getDayConfigs().get(0).getSessionType();
            } else {
                globalSessionType = "ALL_DAY";
            }
        }

        String dayConfigsJson = null;
        try {
            dayConfigsJson = objectMapper.writeValueAsString(dto.getDayConfigs());
        } catch (Exception e) {
            log.error("Lỗi serialize dayConfigs trong update: {}", e.getMessage());
        }

        // Tự động tính toán lại các trường đại diện
        String globalMakeupOption = dto.getMakeupOption();
        LocalDate globalMakeupDate = dto.getMakeupDate();
        Long globalMakeupTimeslotId = dto.getMakeupTimeslotId();

        if (dto.getLessonOptions() != null && !dto.getLessonOptions().isEmpty()) {
            boolean hasMakeup = dto.getLessonOptions().stream()
                    .anyMatch(opt -> "MAKEUP".equals(opt.getMakeupOption()));
            if (hasMakeup) {
                globalMakeupOption = "MAKEUP";
                var firstMakeup = dto.getLessonOptions().stream()
                        .filter(opt -> "MAKEUP".equals(opt.getMakeupOption()))
                        .findFirst().orElse(null);
                if (firstMakeup != null) {
                    globalMakeupDate = firstMakeup.getMakeupDate();
                    globalMakeupTimeslotId = firstMakeup.getMakeupTimeslotId();
                }
            } else {
                globalMakeupOption = "NO_MAKEUP";
                globalMakeupDate = null;
                globalMakeupTimeslotId = null;
            }
        }

        // Cập nhật thông tin trong bảng leave_request
        String updateSql = """
                UPDATE leave_request
                SET from_date = :fromDate,
                    to_date = :toDate,
                    reason = :reason,
                    session_type = :sessionType,
                    day_configs = :dayConfigs,
                    makeup_option = :makeupOption,
                    makeup_date = :makeupDate,
                    makeup_timeslot_id = :makeupTimeslotId,
                    updated_at = NOW()
                WHERE id = :id AND is_deleted = 0
                """;
        jdbc.update(updateSql, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("fromDate", dto.getFromDate())
                .addValue("toDate", dto.getToDate())
                .addValue("reason", dto.getReason())
                .addValue("sessionType", globalSessionType)
                .addValue("dayConfigs", dayConfigsJson)
                .addValue("makeupOption", globalMakeupOption)
                .addValue("makeupDate", globalMakeupDate)
                .addValue("makeupTimeslotId", globalMakeupTimeslotId));

        // Xóa các tùy chọn cũ trong bảng leave_request_lesson
        String deleteOptionsSql = "DELETE FROM leave_request_lesson WHERE leave_request_id = :leaveRequestId";
        jdbc.update(deleteOptionsSql, new MapSqlParameterSource("leaveRequestId", id));

        // Kiểm tra xem giáo viên có tiết học nào trong khoảng thời gian mới này không
        List<Lesson> affected = getAffectedLessons(teacherId, dto.getDayConfigs());

        // Lưu thông tin phương án xử lý chi tiết mới cho từng buổi học
        if (dto.getLessonOptions() != null && !dto.getLessonOptions().isEmpty()) {
            for (LeaveRequestDto.LessonOptionDto opt : dto.getLessonOptions()) {
                String insertOptionSql = """
                        INSERT INTO leave_request_lesson (leave_request_id, lesson_id, makeup_option, makeup_date, makeup_timeslot_id)
                        VALUES (:leaveRequestId, :lessonId, :makeupOption, :makeupDate, :makeupTimeslotId)
                        """;
                jdbc.update(insertOptionSql, new MapSqlParameterSource()
                        .addValue("leaveRequestId", id)
                        .addValue("lessonId", opt.getLessonId())
                        .addValue("makeupOption", opt.getMakeupOption())
                        .addValue("makeupDate", opt.getMakeupDate() != null ? java.sql.Date.valueOf(opt.getMakeupDate()) : null)
                        .addValue("makeupTimeslotId", opt.getMakeupTimeslotId()));
            }
        } else {
            // Fallback
            for (Lesson lesson : affected) {
                String insertOptionSql = """
                        INSERT INTO leave_request_lesson (leave_request_id, lesson_id, makeup_option, makeup_date, makeup_timeslot_id)
                        VALUES (:leaveRequestId, :lessonId, :makeupOption, :makeupDate, :makeupTimeslotId)
                        """;
                jdbc.update(insertOptionSql, new MapSqlParameterSource()
                        .addValue("leaveRequestId", id)
                        .addValue("lessonId", lesson.getId())
                        .addValue("makeupOption", dto.getMakeupOption() != null ? dto.getMakeupOption() : "NO_MAKEUP")
                        .addValue("makeupDate", globalMakeupDate != null ? java.sql.Date.valueOf(globalMakeupDate) : null)
                        .addValue("makeupTimeslotId", globalMakeupTimeslotId));
            }
        }

        lr.setFromDate(dto.getFromDate());
        lr.setToDate(dto.getToDate());
        lr.setReason(dto.getReason());
        lr.setSessionType(globalSessionType);
        lr.setDayConfigs(dayConfigsJson);
        lr.setMakeupOption(globalMakeupOption);
        lr.setMakeupDate(globalMakeupDate);
        lr.setMakeupTimeslotId(globalMakeupTimeslotId);
        
        log.info("Giáo viên {} đã cập nhật đơn xin nghỉ #{} thành công", teacherId, id);
        
        return lr;
    }


    /** Kiểm tra tính khả thi của ca dạy bù dự kiến - tương thích ngược */
    @Transactional(readOnly = true)
    public void checkFeasibility(LocalDate date, Long timeslotId, Long roomId, Long classId, Long teacherId) {
        checkFeasibility(date, timeslotId, roomId, classId, teacherId, null);
    }

    /** Kiểm tra tính khả thi của ca dạy bù dự kiến - có hỗ trợ loại trừ buổi học */
    @Transactional(readOnly = true)
    public void checkFeasibility(LocalDate date, Long timeslotId, Long roomId, Long classId, Long teacherId, Long excludeLessonId) {
        // 1. Kiểm tra phòng học trống
        if (roomId != null) {
            String roomSql = """
                    SELECT COUNT(*) FROM lesson_assignment
                    WHERE room_id = :roomId AND session_date = :date AND timeslot_id = :timeslotId AND is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                roomSql += " AND lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("roomId", roomId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(roomSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                throw new BusinessException("Phòng học đã bị trùng lịch sử dụng vào ca và ngày này");
            }
        }

        // 2. Kiểm tra giáo viên trống lịch dạy
        if (teacherId != null) {
            String teacherSql = """
                    SELECT COUNT(*) FROM lesson_assignment
                    WHERE teacher_id = :teacherId AND session_date = :date AND timeslot_id = :timeslotId AND is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                teacherSql += " AND lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("teacherId", teacherId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(teacherSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                throw new BusinessException("Giáo viên đã có lịch dạy vào ca và ngày dạy bù này");
            }

            // Kiểm tra bận cố định
            String unavailSql = """
                    SELECT COUNT(*) FROM teacher_unavailable
                    WHERE teacher_id = :teacherId AND timeslot_id = :timeslotId
                    """;
            Integer unavail = jdbc.queryForObject(unavailSql, params, Integer.class);
            if (unavail != null && unavail > 0) {
                throw new BusinessException("Giáo viên đã đăng ký bận cố định vào ca học này");
            }

            // Kiểm tra nghỉ phép
            String leaveSql = """
                    SELECT COUNT(*) FROM leave_request
                    WHERE teacher_id = :teacherId AND status = 'APPROVED' AND :date BETWEEN from_date AND to_date AND is_deleted = 0
                    """;
            Integer isOnLeave = jdbc.queryForObject(leaveSql, params, Integer.class);
            if (isOnLeave != null && isOnLeave > 0) {
                throw new BusinessException("Giáo viên đang trong kỳ nghỉ phép đã phê duyệt vào ngày này");
            }
        }

        // 3. Kiểm tra học viên trong lớp trống lịch
        if (classId != null) {
            String classSql = """
                    SELECT COUNT(*) FROM lesson_assignment la
                    INNER JOIN lesson l ON la.lesson_id = l.id
                    WHERE l.class_id = :classId AND la.session_date = :date AND la.timeslot_id = :timeslotId AND la.is_deleted = 0
                    """;
            if (excludeLessonId != null) {
                classSql += " AND la.lesson_id != :excludeLessonId";
            }
            var params = new MapSqlParameterSource()
                    .addValue("classId", classId)
                    .addValue("date", date)
                    .addValue("timeslotId", timeslotId)
                    .addValue("excludeLessonId", excludeLessonId);
            Integer occupied = jdbc.queryForObject(classSql, params, Integer.class);
            if (occupied != null && occupied > 0) {
                throw new BusinessException("Lớp học này đã có một ca học khác được xếp trùng vào ngày/ca này");
            }
        }
    }

    @Transactional(readOnly = true)
    public void checkLeaveRequestFeasibility(Long id, Long makeupRoomId) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));
        List<Lesson> affected = getAffectedLessonsByRequest(id, lr.getTeacherId(), lr.getFromDate(), lr.getToDate(), lr.getSessionType());
        
        java.util.Set<String> classSlots = new java.util.HashSet<>();
        java.util.Set<String> teacherSlots = new java.util.HashSet<>();
        java.util.Set<String> roomSlots = new java.util.HashSet<>();

        for (Lesson lesson : affected) {
            String opt = lesson.getMakeupOption() != null ? lesson.getMakeupOption() : lr.getMakeupOption();
            if ("MAKEUP".equals(opt)) {
                LocalDate mDate = lesson.getMakeupDate() != null ? lesson.getMakeupDate() : lr.getMakeupDate();
                Long mTimeslotId = lesson.getMakeupTimeslotId() != null ? lesson.getMakeupTimeslotId() : lr.getMakeupTimeslotId();

                if (mDate == null || mTimeslotId == null) {
                    throw new BusinessException("Buổi học #" + lesson.getLessonIndex() + " của lớp " + lesson.getClassCode() + " yêu cầu đầy đủ ngày và ca dạy bù dự kiến");
                }

                String slotKey = mDate + "_" + mTimeslotId;
                if (!classSlots.add(lesson.getClassId() + "_" + slotKey)) {
                    throw new BusinessException("Trùng lịch học của lớp (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }
                if (!teacherSlots.add(lr.getTeacherId() + "_" + slotKey)) {
                    throw new BusinessException("Giáo viên trùng lịch dạy (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }
                if (makeupRoomId != null && !roomSlots.add(makeupRoomId + "_" + slotKey)) {
                    throw new BusinessException("Phòng học bị trùng lịch (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }

                checkFeasibility(mDate, mTimeslotId, makeupRoomId, lesson.getClassId(), lr.getTeacherId(), lesson.getId());
            }
        }
    }

    /** Phê duyệt đơn nghỉ phép */
    @Transactional
    public LeaveRequest approve(Long id, Long reviewerId, Long makeupRoomId) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));

        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("Đơn này đã được xử lý rồi (trạng thái: " + lr.getStatus() + ")");
        }

        // Tìm tất cả các buổi dạy thực tế bị ảnh hưởng kèm phương án xử lý tương ứng
        List<Lesson> affected = getAffectedLessonsByRequest(id, lr.getTeacherId(), lr.getFromDate(), lr.getToDate(), lr.getSessionType());

        // 1. Kiểm tra tính khả thi toàn bộ batch ca dạy bù trước khi ghi DB
        java.util.Set<String> classSlots = new java.util.HashSet<>();
        java.util.Set<String> teacherSlots = new java.util.HashSet<>();
        java.util.Set<String> roomSlots = new java.util.HashSet<>();

        for (Lesson lesson : affected) {
            String opt = lesson.getMakeupOption() != null ? lesson.getMakeupOption() : lr.getMakeupOption();
            if ("MAKEUP".equals(opt)) {
                LocalDate mDate = lesson.getMakeupDate() != null ? lesson.getMakeupDate() : lr.getMakeupDate();
                Long mTimeslotId = lesson.getMakeupTimeslotId() != null ? lesson.getMakeupTimeslotId() : lr.getMakeupTimeslotId();

                if (mDate == null || mTimeslotId == null) {
                    throw new BusinessException("Buổi học #" + lesson.getLessonIndex() + " của lớp " + lesson.getClassCode() + " yêu cầu đầy đủ ngày và ca dạy bù dự kiến");
                }
                if (makeupRoomId == null) {
                    throw new BusinessException("Vui lòng chọn phòng học dạy bù để tiến hành phê duyệt");
                }

                String slotKey = mDate + "_" + mTimeslotId;
                if (!classSlots.add(lesson.getClassId() + "_" + slotKey)) {
                    throw new BusinessException("Trùng lịch học của lớp (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }
                if (!teacherSlots.add(lr.getTeacherId() + "_" + slotKey)) {
                    throw new BusinessException("Giáo viên trùng lịch dạy (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }
                if (!roomSlots.add(makeupRoomId + "_" + slotKey)) {
                    throw new BusinessException("Phòng học bị trùng lịch (trùng ca dạy bù khác trong đơn tại ca " + slotKey + ")");
                }

                // Kiểm tra khả năng dạy bù (Feasibility check) - loại trừ chính buổi học đang xét tránh tự trùng
                checkFeasibility(mDate, mTimeslotId, makeupRoomId, lesson.getClassId(), lr.getTeacherId(), lesson.getId());
            }
        }

        // 2. Thực hiện đổi lịch dạy bù hoặc tìm người dạy thay và lưu DB
        for (Lesson lesson : affected) {
            String opt = lesson.getMakeupOption() != null ? lesson.getMakeupOption() : lr.getMakeupOption();
            
            if ("MAKEUP".equals(opt)) {
                LocalDate mDate = lesson.getMakeupDate() != null ? lesson.getMakeupDate() : lr.getMakeupDate();
                Long mTimeslotId = lesson.getMakeupTimeslotId() != null ? lesson.getMakeupTimeslotId() : lr.getMakeupTimeslotId();

                classRepository.backupAndReschedule(
                        lesson.getId(),
                        mDate,
                        mTimeslotId,
                        makeupRoomId,
                        "Dạy bù - Đơn xin nghỉ #" + lr.getId(),
                        lr.getId()
                );
            } else {
                // Trường hợp KHÔNG dạy bù (Tìm người dạy thay)
                // Nếu đã có giáo viên khác nhận dạy thay (qua FCFS claim trước đó), không cần xóa giáo viên nữa
                if (lesson.getTeacherId() != null && !lesson.getTeacherId().equals(lr.getTeacherId())) {
                    log.info("Buổi học #{} đã có giáo viên dạy thay #{} nhận trước đó. Giữ nguyên phân công.", 
                            lesson.getLessonIndex(), lesson.getTeacherId());
                } else {
                    // Xóa giáo viên cũ khỏi buổi học này, sao lưu lịch gốc và đánh dấu cần tìm người thay
                    classRepository.backupAndRemoveTeacher(
                            lesson.getId(),
                            "Tìm người dạy thay - Đơn xin nghỉ #" + lr.getId(),
                            lr.getId()
                    );
                }
            }
        }

        log.info("Phê duyệt đơn nghỉ phép #{} thành công cho giáo viên {}. Tổng số buổi: {}", 
                lr.getId(), lr.getTeacherId(), affected.size());

        // Cập nhật trạng thái đơn xin nghỉ
        leaveRequestRepository.updateStatus(id, "APPROVED", reviewerId);
        lr.setStatus("APPROVED");

        // Gửi email thông báo cho giáo viên xin nghỉ
        teacherRepository.findById(lr.getTeacherId()).ifPresent(teacher ->
                notificationService.sendLeaveApprovedNotification(teacher.getEmail(), teacher.getFullName(), lr));

        return lr;
    }

    @Transactional
    public LeaveRequest reject(Long id, Long reviewerId) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));

        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("Đơn này đã được xử lý rồi");
        }

        leaveRequestRepository.updateStatus(id, "REJECTED", reviewerId);
        lr.setStatus("REJECTED");

        teacherRepository.findById(lr.getTeacherId()).ifPresent(teacher ->
                notificationService.sendLeaveRejectedNotification(teacher.getEmail(), teacher.getFullName(), lr));

        return lr;
    }

    /** Tạo và gửi yêu cầu dạy thay hàng loạt (FCFS Substitute Dispatch) */
    @Transactional
    public void dispatchSubstituteOffers(Long leaveRequestId, Long lessonId, List<Long> teacherIds) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", leaveRequestId));

        // Lấy thông tin lớp học và buổi học
        String querySql = """
                SELECT COALESCE(c.class_code, c.name) AS class_code, l.lesson_index, la.session_date, ts.label AS timeslot_label
                FROM lesson l
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN lesson_assignment la ON l.id = la.lesson_id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                WHERE l.id = :lessonId
                """;
        var lessonInfo = jdbc.queryForMap(querySql, new MapSqlParameterSource("lessonId", lessonId));
        String classCode = (String) lessonInfo.get("class_code");
        int index = ((Number) lessonInfo.get("lesson_index")).intValue();
        LocalDate date = lessonInfo.get("session_date") != null ? ((java.sql.Date) lessonInfo.get("session_date")).toLocalDate() : null;
        String timeslotLabel = (String) lessonInfo.get("timeslot_label");

        // Hạn chót token là 24 tiếng sau
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        for (Long tId : teacherIds) {
            Teacher substitute = teacherRepository.findById(tId)
                    .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", tId));

            // Sinh secure token duy nhất
            String token = UUID.randomUUID().toString();

            SubstituteOffer offer = SubstituteOffer.builder()
                    .leaveRequestId(leaveRequestId)
                    .lessonId(lessonId)
                    .teacherId(tId)
                    .token(token)
                    .status("PENDING")
                    .expiresAt(expiresAt)
                    .build();

            substituteOfferRepository.save(offer);

            // Gửi email kèm link claim FCFS thông qua notificationService
            String claimUrl = "http://localhost:3000/substitute/confirm?token=" + token;
            String dateStr = date != null ? date.toString() : "-";

            try {
                log.info("Đang gửi email FCFS mời dạy thay tới {} ({}) - Token: {}", substitute.getFullName(), substitute.getEmail(), token);
                notificationService.sendSubstituteOfferNotification(
                        substitute.getEmail(),
                        substitute.getFullName(),
                        classCode,
                        index,
                        dateStr,
                        timeslotLabel,
                        claimUrl
                );
            } catch (Exception e) {
                log.error("Lỗi gửi email mời dạy thay tới {}: {}", substitute.getEmail(), e.getMessage());
            }
        }
    }

    /**
     * Lấy danh sách các phòng học trống vào các ca/ngày dạy bù của đơn và đủ sức chứa.
     */
    @Transactional(readOnly = true)
    public List<Room> getAvailableRoomsForRequest(Long leaveRequestId) {
        LeaveRequest lr = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", leaveRequestId));

        // Lấy tất cả các buổi dạy thực tế bị ảnh hưởng
        List<Lesson> affected = getAffectedLessonsByRequest(
                leaveRequestId, lr.getTeacherId(), lr.getFromDate(), lr.getToDate(), lr.getSessionType());

        // Lọc ra các ca dạy bù
        List<Lesson> makeupLessons = affected.stream()
                .filter(l -> "MAKEUP".equals(l.getMakeupOption()))
                .toList();

        if (makeupLessons.isEmpty()) {
            return roomRepository.findAll();
        }

        // Tìm tối đa sĩ số lớp của các ca dạy bù
        int maxStudents = 0;
        for (Lesson l : makeupLessons) {
            String studentCountSql = "SELECT COALESCE(student_count, 0) FROM class WHERE id = :classId";
            Integer sc = jdbc.queryForObject(studentCountSql, new MapSqlParameterSource("classId", l.getClassId()), Integer.class);
            if (sc != null && sc > maxStudents) {
                maxStudents = sc;
            }
        }

        // Xây dựng danh sách các slot bận cần check
        List<java.util.Map<String, Object>> slots = new java.util.ArrayList<>();
        for (Lesson l : makeupLessons) {
            if (l.getMakeupDate() != null && l.getMakeupTimeslotId() != null) {
                java.util.Map<String, Object> slot = new java.util.HashMap<>();
                slot.put("date", java.sql.Date.valueOf(l.getMakeupDate()));
                slot.put("timeslotId", l.getMakeupTimeslotId());
                slots.add(slot);
            }
        }

        return roomRepository.findAvailableRoomsForSlots(maxStudents, slots);
    }
}
