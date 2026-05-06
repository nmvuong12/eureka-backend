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
        List<Lesson> lessons = classRepository.findAllLessonsForSolver();
        List<Long> teacherIds = teacherRepository.findAllActiveIds();
        List<Long> roomIds = roomRepository.findAllActiveIds();
        List<Long> timeslotIds = timeslotRepository.findAllIds();
        List<Timetable.TeacherFact> teacherFacts = teacherIds.stream()
                .map(id -> new Timetable.TeacherFact(id, "ACTIVE")).toList();
        List<Timetable.RoomFact> roomFacts = roomRepository.findAllRoomFacts();
        List<Timetable.TimeslotFact> timeslotFacts = timeslotRepository.findAllTimeslotFacts();
        List<Timetable.TeacherUnavailableFact> unavailableFacts = timeslotRepository.findAllTeacherUnavailableFacts();
        List<Timetable.TeacherSkillFact> skillFacts = teacherRepository.findAllSkillFacts();

        return new Timetable("EurekaSchedule", teacherIds, roomIds, timeslotIds,
                teacherFacts, roomFacts, timeslotFacts, unavailableFacts, skillFacts, lessons, null);
    }

    @Transactional
    public void saveSolution(Timetable solution) {
        classRepository.batchUpdateAssignments(solution.getLessons());
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> getTimetable(Long teacherId, Long classId, Long roomId) {
        String sql = """
                SELECT l.id AS lesson_id, l.class_id, c.name AS class_name, l.lesson_index, l.required_skill,
                       la.teacher_id, t.name AS teacher_name,
                       la.room_id, r.name AS room_name,
                       la.timeslot_id, ts.day_of_week, ts.start_time, ts.end_time, ts.label AS timeslot_label,
                       COALESCE(la.is_pinned, 0) AS pinned
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                INNER JOIN class c ON l.class_id = c.id
                LEFT JOIN teacher t ON la.teacher_id = t.id
                LEFT JOIN room r ON la.room_id = r.id
                LEFT JOIN timeslot ts ON la.timeslot_id = ts.id
                WHERE la.timeslot_id IS NOT NULL
                  AND (:teacherId IS NULL OR la.teacher_id = :teacherId)
                  AND (:classId IS NULL OR l.class_id = :classId)
                  AND (:roomId IS NULL OR la.room_id = :roomId)
                ORDER BY FIELD(ts.day_of_week,'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'),
                         ts.start_time, c.name, l.lesson_index
                """;
        var params = new MapSqlParameterSource()
                .addValue("teacherId", teacherId)
                .addValue("classId", classId)
                .addValue("roomId", roomId);

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
}
