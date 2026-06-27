package com.eureka.timetabling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO chứa thông tin thống kê số liệu trên trang Dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private long totalTeachers;
    private long activeClasses;
    private long totalRooms;
    private long todaySessions;
    private long pendingLeaveRequests;
    private List<Integer> weeklySessions; // Thống kê số lượng ca học từ Thứ 2 đến Chủ nhật
    private Map<String, Long> teacherTypeDistribution; // Phân bố loại giáo viên (FULL_TIME, PART_TIME)
    private Map<String, Long> classStatusDistribution; // Phân bố số lớp theo khóa học
}
