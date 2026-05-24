package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.Course;
import com.eureka.timetabling.domain.SchedulePattern;
import com.eureka.timetabling.dto.response.CapacityDashboardItem;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service tính toán Capacity Dashboard dựa trên nguồn lực thực tế (Resource-Based).
 * Không dùng AI - Tính chính xác dựa trên số giáo viên hoạt động có skill phù hợp và phòng trống.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CapacityCalculationService {

    private final CourseRepository courseRepository;
    private final SchedulePatternRepository schedulePatternRepository;
    private final ClassRepository classRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;

    /**
     * Tính toán Capacity Dashboard cho tất cả pattern đang hoạt động dựa trên kỹ năng yêu cầu của Khóa học.
     *
     * @param courseId ID của khóa học để lấy kỹ năng yêu cầu
     * @return Danh sách CapacityDashboardItem
     */
    @Transactional(readOnly = true)
    public List<CapacityDashboardItem> calculateDashboard(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", courseId));

        List<SchedulePattern> activePatterns = schedulePatternRepository.findAllActive();
        List<CapacityDashboardItem> dashboard = new ArrayList<>();

        for (SchedulePattern pattern : activePatterns) {
            dashboard.add(calculateForPattern(pattern, course.getRequiredSkillCode()));
        }

        return dashboard;
    }

    /**
     * Tính toán chi tiết Capacity cho một Schedule Pattern và một khóa học cụ thể.
     */
    @Transactional(readOnly = true)
    public CapacityDashboardItem calculateForPattern(Long patternId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", courseId));

        SchedulePattern pattern = schedulePatternRepository.findById(patternId)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", patternId));

        return calculateForPattern(pattern, course.getRequiredSkillCode());
    }

    private CapacityDashboardItem calculateForPattern(SchedulePattern pattern, String requiredSkillCode) {
        Long patternId = pattern.getId();

        // 1. Tìm các phòng và giáo viên đã bị bận vào pattern này (trong các lớp đang OPEN hoặc STUDYING)
        List<Long> assignedRoomIds = classRepository.findAssignedRoomIdsByPattern(patternId);
        List<Long> assignedTeacherIds = classRepository.findAssignedTeacherIdsByPattern(patternId);

        // 2. Tính số lượng phòng ACTIVE và chưa bận
        int availableRooms = roomRepository.countAvailableRooms(assignedRoomIds);

        // 3. Tính số lượng GV ACTIVE, có skill phù hợp và chưa bận
        int availableTeachers = teacherRepository.countAvailableTeachers(assignedTeacherIds, requiredSkillCode);

        // 4. Công suất tối đa = MIN(phòng trống, giáo viên rảnh phù hợp skill)
        int capacity = Math.min(availableRooms, availableTeachers);

        // 5. Số lớp đang sử dụng pattern này (OPEN hoặc STUDYING)
        int occupied = classRepository.countByPatternAndActiveStatus(patternId);

        // 6. Số lượng slot còn có thể mở lớp = capacity - occupied
        int remaining = Math.max(0, capacity - occupied);

        return CapacityDashboardItem.builder()
                .patternId(patternId)
                .patternCode(pattern.getCode())
                .studyDays(pattern.getStudyDays())
                .slotCode(pattern.getSlotCode())
                .slotStart(pattern.getSlotStart())
                .slotEnd(pattern.getSlotEnd())
                .sessionsPerWeek(pattern.getSessionsPerWeek())
                .displayLabel(pattern.getDisplayLabel())
                .capacity(capacity)
                .occupied(occupied)
                .remaining(remaining)
                .availableRooms(availableRooms)
                .availableTeachers(availableTeachers)
                .build();
    }
}
