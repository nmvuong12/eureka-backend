package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.dto.response.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Service tính toán thống kê và thu thập số liệu thật cho Dashboard
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        // 1. Tổng số giáo viên đang hoạt động
        String teachersSql = "SELECT COUNT(*) FROM teacher WHERE working_status = 'ACTIVE' AND is_deleted = 0";
        Long totalTeachers = jdbc.queryForObject(teachersSql, new MapSqlParameterSource(), Long.class);
        if (totalTeachers == null) totalTeachers = 0L;

        // 2. Lớp học đang mở (status = OPEN hoặc STUDYING)
        String classesSql = "SELECT COUNT(*) FROM class WHERE status IN ('OPEN', 'STUDYING') AND is_deleted = 0";
        Long activeClasses = jdbc.queryForObject(classesSql, new MapSqlParameterSource(), Long.class);
        if (activeClasses == null) activeClasses = 0L;

        // 3. Tổng số phòng học đang hoạt động
        String roomsSql = "SELECT COUNT(*) FROM room WHERE status = 'ACTIVE' AND is_deleted = 0";
        Long totalRooms = jdbc.queryForObject(roomsSql, new MapSqlParameterSource(), Long.class);
        if (totalRooms == null) totalRooms = 0L;

        // 4. Số ca học hôm nay
        LocalDate today = LocalDate.now();
        String todaySessionsSql = "SELECT COUNT(*) FROM lesson_assignment WHERE session_date = :today";
        Long todaySessions = jdbc.queryForObject(todaySessionsSql, new MapSqlParameterSource("today", java.sql.Date.valueOf(today)), Long.class);
        if (todaySessions == null) todaySessions = 0L;

        // 5. Đơn xin nghỉ đang chờ duyệt
        String pendingLeavesSql = "SELECT COUNT(*) FROM leave_request WHERE status = 'PENDING' AND is_deleted = 0";
        Long pendingLeaveRequests = jdbc.queryForObject(pendingLeavesSql, new MapSqlParameterSource(), Long.class);
        if (pendingLeaveRequests == null) pendingLeaveRequests = 0L;

        // 6. Số lượng ca học trong tuần này (Thứ 2 - Chủ nhật)
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        
        String weeklySessionsSql = """
                SELECT session_date, COUNT(*) as count 
                FROM lesson_assignment 
                WHERE session_date BETWEEN :monday AND :sunday
                GROUP BY session_date
                """;
        var weeklyParams = new MapSqlParameterSource()
                .addValue("monday", java.sql.Date.valueOf(monday))
                .addValue("sunday", java.sql.Date.valueOf(sunday));
        
        List<Map<String, Object>> weeklyRows = jdbc.queryForList(weeklySessionsSql, weeklyParams);
        Map<LocalDate, Integer> weeklyMap = new HashMap<>();
        for (Map<String, Object> row : weeklyRows) {
            LocalDate date = ((java.sql.Date) row.get("session_date")).toLocalDate();
            Number countNum = (Number) row.get("count");
            weeklyMap.put(date, countNum.intValue());
        }

        List<Integer> weeklySessions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            weeklySessions.add(weeklyMap.getOrDefault(day, 0));
        }

        // 7. Phân bố loại giáo viên (FULL_TIME, PART_TIME)
        String teacherTypeSql = """
                SELECT teacher_type, COUNT(*) as count 
                FROM teacher 
                WHERE is_deleted = 0 
                GROUP BY teacher_type
                """;
        List<Map<String, Object>> teacherTypeRows = jdbc.queryForList(teacherTypeSql, new MapSqlParameterSource());
        Map<String, Long> teacherTypeDistribution = new HashMap<>();
        for (Map<String, Object> row : teacherTypeRows) {
            String type = (String) row.get("teacher_type");
            Number countNum = (Number) row.get("count");
            if (type != null) {
                teacherTypeDistribution.put(type, countNum.longValue());
            }
        }

        // 8. Phân bố lớp học theo khóa học (IELTS, TOEIC, v.v.)
        String courseDistributionSql = """
                SELECT COALESCE(co.name, 'Chưa xếp khóa') as name, COUNT(cl.id) as count
                FROM class cl
                LEFT JOIN course_batch cb ON cl.batch_id = cb.id
                LEFT JOIN course co ON cb.course_id = co.id OR cl.course_id = co.id
                WHERE cl.is_deleted = 0
                GROUP BY co.name
                """;
        List<Map<String, Object>> courseRows = jdbc.queryForList(courseDistributionSql, new MapSqlParameterSource());
        Map<String, Long> courseDistribution = new HashMap<>();
        for (Map<String, Object> row : courseRows) {
            String name = (String) row.get("name");
            Number countNum = (Number) row.get("count");
            courseDistribution.put(name, countNum.longValue());
        }

        return DashboardStatsResponse.builder()
                .totalTeachers(totalTeachers)
                .activeClasses(activeClasses)
                .totalRooms(totalRooms)
                .todaySessions(todaySessions)
                .pendingLeaveRequests(pendingLeaveRequests)
                .weeklySessions(weeklySessions)
                .teacherTypeDistribution(teacherTypeDistribution)
                .classStatusDistribution(courseDistribution)
                .build();
    }
}
