package com.eureka.timetabling.service.impl;

import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.dto.response.TimetableEntryResponse;
import com.eureka.timetabling.repository.*;
import com.eureka.timetabling.solver.Timetable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service điều phối Timefold Solver để tạo thời khóa biểu tối ưu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableService {

    private final SolverFactory<Timetable> solverFactory;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final TimeslotRepository timeslotRepository;
    private final SchedulePatternRepository schedulePatternRepository;
    private final SkillRepository skillRepository;
    private final ConstraintConfigRepository constraintConfigRepository;
    private final NamedParameterJdbcTemplate jdbc;

    // Trạng thái giải - dùng cho API polling
    private final ConcurrentHashMap<String, String> solverStatus = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<String> solveAsync(String jobId) {
        solverStatus.put(jobId, "SOLVING");
        try {
            Timetable problem = buildProblem();
            log.info("Bắt đầu xếp lịch - Tổng buổi học: {}, Giáo viên: {}, Phòng: {}, Ca: {}",
                    problem.getLessons().size(),
                    problem.getTeacherIds().size(),
                    problem.getRoomIds().size(),
                    problem.getTimeslotIds().size());

            Solver<Timetable> solver = solverFactory.buildSolver();
            Timetable solution = solver.solve(problem);

            log.info("Xếp lịch hoàn thành - Score: {}", solution.getScore());
            saveSolution(solution);
            solverStatus.put(jobId, "DONE:" + solution.getScore());
            return CompletableFuture.completedFuture(jobId);
        } catch (Exception e) {
            log.error("Lỗi xếp lịch: {}", e.getMessage(), e);
            solverStatus.put(jobId, "ERROR:" + e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public String getSolverStatus(String jobId) {
        return solverStatus.getOrDefault(jobId, "NOT_FOUND");
    }

    private Timetable buildProblem() {
        // Đồng bộ hóa danh mục kỹ năng phục vụ đối chiếu phân cấp trình độ trong Solver
        List<com.eureka.timetabling.domain.Skill> activeSkills = skillRepository.findAll();
        java.util.Map<String, com.eureka.timetabling.solver.SkillMetadataHolder.SkillInfo> skillMetaMap = new java.util.HashMap<>();
        for (com.eureka.timetabling.domain.Skill s : activeSkills) {
            if (s.getSkillGroup() != null && s.getLevelRank() != null) {
                skillMetaMap.put(s.getSkillCode(), new com.eureka.timetabling.solver.SkillMetadataHolder.SkillInfo(s.getSkillGroup(), s.getLevelRank()));
            }
        }
        com.eureka.timetabling.solver.SkillMetadataHolder.setSkills(skillMetaMap);

        List<Lesson> lessons = classRepository.findAllLessonsForSolver();
        List<com.eureka.timetabling.domain.Teacher> activeTeachers = teacherRepository.findAll("ACTIVE");
        List<Long> teacherIds = activeTeachers.stream().map(com.eureka.timetabling.domain.Teacher::getId).toList();
        List<Long> roomIds = roomRepository.findAllActiveIds();
        List<Long> timeslotIds = timeslotRepository.findAllIds();
        List<Timetable.TeacherFact> teacherFacts = activeTeachers.stream()
                .map(t -> new Timetable.TeacherFact(t.getId(), "ACTIVE", t.getTeacherType().name())).toList();
        List<Timetable.RoomFact> roomFacts = roomRepository.findAllRoomFacts();
        List<Timetable.TimeslotFact> timeslotFacts = timeslotRepository.findAllTimeslotFacts();
        List<Timetable.TeacherUnavailableFact> unavailableFacts = timeslotRepository.findAllTeacherUnavailableFacts();
        List<Timetable.TeacherSkillFact> skillFacts = teacherRepository.findAllSkillFacts();
        List<com.eureka.timetabling.domain.SchedulePattern> schedulePatterns = schedulePatternRepository.findAllActive();
        List<com.eureka.timetabling.domain.ConstraintConfig> constraintConfigs = constraintConfigRepository.findAll();

        // Đồng bộ hóa cấu hình các ràng buộc mềm động và thông tin bổ trợ trong TimetableConstraintProvider
        String timeslotSql = "SELECT id, day_of_week, start_time FROM timeslot";
        List<com.eureka.timetabling.solver.TimetableConstraintProvider.TimeslotInfo> timeslotInfos = jdbc.getJdbcTemplate().query(
                timeslotSql, 
                (rs, rowNum) -> new com.eureka.timetabling.solver.TimetableConstraintProvider.TimeslotInfo(
                        rs.getLong("id"), 
                        rs.getString("day_of_week"), 
                        rs.getString("start_time")
                )
        );
        java.util.Map<Long, String> teacherTypeMap = activeTeachers.stream()
                .collect(java.util.stream.Collectors.toMap(com.eureka.timetabling.domain.Teacher::getId, t -> t.getTeacherType().name()));
        com.eureka.timetabling.solver.TimetableConstraintProvider.initSolverData(constraintConfigs, timeslotInfos, teacherTypeMap);

        return new Timetable("EurekaSchedule", teacherIds, roomIds, timeslotIds,
                teacherFacts, roomFacts, timeslotFacts, unavailableFacts, skillFacts, schedulePatterns,
                constraintConfigs, lessons, null);
    }

    @Transactional
    public void saveSolution(Timetable solution) {
        // 1. Lưu các buổi học đại diện đã xếp lịch
        classRepository.batchUpdateAssignments(solution.getLessons());

        // 2. Tự động lan truyền (propagate) và tính ngày dương lịch cụ thể (session_date)
        java.util.Set<Long> processedClasses = new java.util.HashSet<>();
        
        for (Lesson solvedLesson : solution.getLessons()) {
            Long classId = solvedLesson.getClassId();
            if (classId == null || processedClasses.contains(classId)) {
                continue;
            }
            processedClasses.add(classId);

            // Truy vấn thông tin lớp học và lịch chuẩn (schedule pattern)
            try {
                String classSql = """
                        SELECT c.id, c.start_date, c.actual_opening_date,
                               sp.id AS pattern_id, sp.study_days, sp.sessions_per_week
                        FROM class c
                        INNER JOIN schedule_pattern sp ON c.schedule_pattern_id = sp.id
                        WHERE c.id = :classId
                        """;
                java.util.Map<String, Object> classInfo = jdbc.queryForMap(classSql, new MapSqlParameterSource("classId", classId));
                if (classInfo != null) {
                    java.sql.Date sqlStartDate = (java.sql.Date) (classInfo.get("actual_opening_date") != null 
                            ? classInfo.get("actual_opening_date") 
                            : classInfo.get("start_date"));
                    String studyDays = (String) classInfo.get("study_days");
                    int K = ((Number) classInfo.get("sessions_per_week")).intValue();

                    if (sqlStartDate != null && studyDays != null) {
                        java.time.LocalDate startDate = sqlStartDate.toLocalDate();

                        // Lấy tổng số buổi học thực tế của lớp
                        String countSql = "SELECT COUNT(*) FROM lesson WHERE class_id = :classId AND is_deleted = 0";
                        Integer totalLessons = jdbc.queryForObject(countSql, new MapSqlParameterSource("classId", classId), Integer.class);
                        if (totalLessons == null) totalLessons = 24;

                        // Tính chính xác ngày dương lịch cho từng buổi học
                        List<java.time.LocalDate> lessonDates = calculateLessonDates(startDate, studyDays, totalLessons);

                        // Cập nhật ngày dương lịch cho tất cả các buổi học của lớp (INSERT mới hoặc cập nhật nếu có sẵn)
                        for (int i = 0; i < lessonDates.size(); i++) {
                            int lessonIndex = i + 1;
                            java.time.LocalDate date = lessonDates.get(i);
                            String dateSql = """
                                    INSERT INTO lesson_assignment (lesson_id, session_date)
                                    SELECT l.id, :sessionDate
                                    FROM lesson l
                                    WHERE l.class_id = :classId AND l.lesson_index = :lessonIndex
                                    ON DUPLICATE KEY UPDATE
                                      session_date = VALUES(session_date),
                                      updated_at = NOW()
                                    """;
                            jdbc.update(dateSql, new MapSqlParameterSource()
                                    .addValue("sessionDate", java.sql.Date.valueOf(date))
                                    .addValue("classId", classId)
                                    .addValue("lessonIndex", lessonIndex));
                        }
                    }
 
                    // Lan truyền giáo viên, phòng và ca học từ K buổi đại diện sang các buổi còn lại
                    // Đảm bảo tạo mới bản ghi trong lesson_assignment nếu chưa tồn tại (INSERT ... ON DUPLICATE KEY UPDATE)
                    for (Lesson l : solution.getLessons()) {
                        if (classId.equals(l.getClassId())) {
                            int r = l.getLessonIndex();
                            String propagateSql = """
                                    INSERT INTO lesson_assignment (lesson_id, teacher_id, room_id, timeslot_id)
                                    SELECT l.id, :teacherId, :roomId, :timeslotId
                                    FROM lesson l
                                    WHERE l.class_id = :classId
                                      AND l.lesson_index > :K
                                      AND MOD(l.lesson_index - :r, :K) = 0
                                    ON DUPLICATE KEY UPDATE
                                      teacher_id = IF(is_pinned = 0, :teacherId, teacher_id),
                                      room_id = IF(is_pinned = 0, :roomId, room_id),
                                      timeslot_id = IF(is_pinned = 0, :timeslotId, timeslot_id),
                                      updated_at = NOW()
                                    """;
                            MapSqlParameterSource params = new MapSqlParameterSource()
                                    .addValue("teacherId", l.getTeacherId())
                                    .addValue("roomId", l.getRoomId())
                                    .addValue("timeslotId", l.getTimeslotId())
                                    .addValue("classId", classId)
                                    .addValue("K", K)
                                    .addValue("r", r);
                            jdbc.update(propagateSql, params);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Lỗi lan truyền và tính ngày thời khóa biểu cho classId: {}", classId, e);
            }
        }
    }

    /**
     * Thuật toán tính ngày dương lịch cụ thể cho các buổi học dựa trên ngày bắt đầu và các ngày học trong tuần
     */
    private List<java.time.LocalDate> calculateLessonDates(java.time.LocalDate startDate, String studyDays, int totalLessons) {
        List<java.time.LocalDate> dates = new java.util.ArrayList<>();
        if (startDate == null || studyDays == null || totalLessons <= 0) return dates;

        java.util.Set<java.time.DayOfWeek> activeDays = new java.util.HashSet<>();
        for (String d : studyDays.split(",")) {
            String trimmed = d.trim();
            if (trimmed.equals("2")) activeDays.add(java.time.DayOfWeek.MONDAY);
            else if (trimmed.equals("3")) activeDays.add(java.time.DayOfWeek.TUESDAY);
            else if (trimmed.equals("4")) activeDays.add(java.time.DayOfWeek.WEDNESDAY);
            else if (trimmed.equals("5")) activeDays.add(java.time.DayOfWeek.THURSDAY);
            else if (trimmed.equals("6")) activeDays.add(java.time.DayOfWeek.FRIDAY);
            else if (trimmed.equals("7")) activeDays.add(java.time.DayOfWeek.SATURDAY);
            else if (trimmed.equals("1")) activeDays.add(java.time.DayOfWeek.SUNDAY);
        }

        java.time.LocalDate current = startDate;
        while (dates.size() < totalLessons) {
            if (activeDays.contains(current.getDayOfWeek())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }
        return dates;
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getTimetable(Long teacherId, Long classId, Long roomId) {
        return getTimetable(teacherId, classId, roomId, null);
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getTimetable(Long teacherId, Long classId, Long roomId, Long batchId) {
        String sql = """
                SELECT l.id AS lesson_id, l.class_id, c.name AS class_name, l.lesson_index, l.required_skill,
                       COALESCE(lr.teacher_id, la.teacher_id) AS teacher_id, t.full_name AS teacher_name,
                       COALESCE(la.original_room_id, la.room_id) AS room_id, r.name AS room_name,
                       COALESCE(la.original_timeslot_id, la.timeslot_id) AS timeslot_id, ts.day_of_week, ts.start_time, ts.end_time, ts.label AS timeslot_label,
                       COALESCE(la.is_pinned, 0) AS pinned
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                INNER JOIN schedule_pattern sp ON c.schedule_pattern_id = sp.id
                LEFT JOIN leave_request lr ON la.leave_request_id = lr.id
                LEFT JOIN teacher t ON COALESCE(lr.teacher_id, la.teacher_id) = t.id
                LEFT JOIN room r ON COALESCE(la.original_room_id, la.room_id) = r.id
                LEFT JOIN timeslot ts ON COALESCE(la.original_timeslot_id, la.timeslot_id) = ts.id
                WHERE la.timeslot_id IS NOT NULL
                  AND l.lesson_index <= sp.sessions_per_week
                  AND (:teacherId IS NULL OR COALESCE(lr.teacher_id, la.teacher_id) = :teacherId)
                  AND (:classId IS NULL OR l.class_id = :classId)
                  AND (:roomId IS NULL OR COALESCE(la.original_room_id, la.room_id) = :roomId)
                  AND (:batchId IS NULL OR c.batch_id = :batchId)
                ORDER BY FIELD(ts.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
                         ts.start_time, c.name, l.lesson_index
                """;
        var params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("classId", classId)
                .addValue("roomId", roomId)
                .addValue("batchId", batchId);

        return jdbc.query(sql, params, (rs, row) -> TimetableEntryResponse.builder()
                .lessonId(rs.getLong("lesson_id"))
                .classId(rs.getLong("class_id"))
                .className(rs.getString("class_name"))
                .lessonIndex(rs.getInt("lesson_index"))
                .requiredSkill(rs.getString("required_skill"))
                .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
                .teacherName(rs.getString("teacher_name"))
                .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
                .roomName(rs.getString("room_name"))
                .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
                .dayOfWeek(rs.getString("day_of_week"))
                .startTime(rs.getString("start_time"))
                .endTime(rs.getString("end_time"))
                .timeslotLabel(rs.getString("timeslot_label"))
                .pinned(rs.getBoolean("pinned"))
                .build());
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getWeeklyCalendar(java.time.LocalDate startDate, java.time.LocalDate endDate, Long teacherId, Long classId, Long roomId, Long batchId) {
        String sql = """
                SELECT l.id AS lesson_id, l.class_id, c.name AS class_name, l.lesson_index, l.required_skill,
                       la.teacher_id, t.full_name AS teacher_name,
                       la.room_id, r.name AS room_name,
                       la.timeslot_id, ts.day_of_week, ts.start_time, ts.end_time, ts.label AS timeslot_label,
                       la.session_date,
                       COALESCE(la.is_pinned, 0) AS pinned
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN teacher t ON la.teacher_id = t.id
                LEFT JOIN room r ON la.room_id = r.id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                WHERE la.timeslot_id IS NOT NULL
                  AND la.session_date >= :startDate
                  AND la.session_date <= :endDate
                  AND (:teacherId IS NULL OR la.teacher_id = :teacherId)
                  AND (:classId IS NULL OR l.class_id = :classId)
                  AND (:roomId IS NULL OR la.room_id = :roomId)
                  AND (:batchId IS NULL OR c.batch_id = :batchId)
                ORDER BY la.session_date, ts.start_time, c.name, l.lesson_index
                """;
        var params = new MapSqlParameterSource()
                .addValue("startDate", java.sql.Date.valueOf(startDate))
                .addValue("endDate", java.sql.Date.valueOf(endDate))
                .addValue("teacherId", teacherId)
                .addValue("classId", classId)
                .addValue("roomId", roomId)
                .addValue("batchId", batchId);

        return jdbc.query(sql, params, (rs, row) -> {
            java.sql.Date sqlDate = rs.getDate("session_date");
            return TimetableEntryResponse.builder()
                    .lessonId(rs.getLong("lesson_id"))
                    .classId(rs.getLong("class_id"))
                    .className(rs.getString("class_name"))
                    .lessonIndex(rs.getInt("lesson_index"))
                    .requiredSkill(rs.getString("required_skill"))
                    .teacherId(rs.getObject("teacher_id") != null ? rs.getLong("teacher_id") : null)
                    .teacherName(rs.getString("teacher_name"))
                    .roomId(rs.getObject("room_id") != null ? rs.getLong("room_id") : null)
                    .roomName(rs.getString("room_name"))
                    .timeslotId(rs.getObject("timeslot_id") != null ? rs.getLong("timeslot_id") : null)
                    .dayOfWeek(rs.getString("day_of_week"))
                    .startTime(rs.getString("start_time"))
                    .endTime(rs.getString("end_time"))
                    .timeslotLabel(rs.getString("timeslot_label"))
                    .pinned(rs.getBoolean("pinned"))
                    .sessionDate(sqlDate != null ? sqlDate.toLocalDate() : null)
                    .build();
        });
    }

    @Transactional
    public int lockTimetable(Long batchId) {
        // 1. Ghim tất cả các buổi học đã được xếp ca của các lớp đang ở trạng thái OPEN hoặc STUDYING
        String pinSql = """
                UPDATE lesson_assignment la
                INNER JOIN lesson l ON la.lesson_id = l.id
                INNER JOIN class c ON l.class_id = c.id
                SET la.is_pinned = 1, la.updated_at = NOW()
                WHERE la.timeslot_id IS NOT NULL
                  AND c.status IN ('OPEN', 'STUDYING')
                  AND c.is_deleted = 0
                  AND (:batchId IS NULL OR c.batch_id = :batchId)
                """;
        int pinnedCount = jdbc.update(pinSql, new MapSqlParameterSource("batchId", batchId));

        // 2. Tự động chuyển đổi trạng thái lớp từ OPEN sang STUDYING
        String classSql = """
                UPDATE class
                SET status = 'STUDYING', updated_at = NOW()
                WHERE status = 'OPEN'
                  AND is_deleted = 0
                  AND (:batchId IS NULL OR batch_id = :batchId)
                """;
        jdbc.update(classSql, new MapSqlParameterSource("batchId", batchId));
        
        return pinnedCount;
    }
}
