package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.Course;
import com.eureka.timetabling.domain.CourseStatus;
import com.eureka.timetabling.dto.request.CourseRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service quản lý danh mục khóa học (Course Catalog).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public PageResponse<Course> search(String query, String status, int page, int size) {
        List<Course> content = courseRepository.searchPaged(query, status, page, size);
        long total = courseRepository.countSearch(query, status);
        return PageResponse.of(content, page, size, total);
    }

    @Transactional(readOnly = true)
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course findById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", id));
    }

    @Transactional
    public Course create(CourseRequest request) {
        if (courseRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Mã khóa học '" + request.getCode() + "' đã tồn tại trong hệ thống");
        }
        if (request.getMinStudents() >= request.getMaxStudents()) {
            throw new IllegalArgumentException("Sĩ số tối thiểu phải nhỏ hơn sĩ số tối đa");
        }

        Course course = Course.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .totalSessions(request.getTotalSessions())
                .totalLessons(request.getTotalSessions()) // giữ đồng bộ legacy
                .sessionsPerWeek(request.getSessionsPerWeek())
                .durationWeeks(request.getDurationWeeks())
                .minStudents(request.getMinStudents())
                .maxStudents(request.getMaxStudents())
                .tuitionFee(request.getTuitionFee() != null ? request.getTuitionFee() : BigDecimal.ZERO)
                .requiredSkillCode(request.getRequiredSkillCode())
                .status(CourseStatus.ACTIVE)
                .defaultDuration(request.getDefaultDuration() != null ? request.getDefaultDuration() : 120)
                .build();

        Long id = courseRepository.save(course);
        course.setId(id);
        log.info("Đã tạo khóa học thành công: {} (code: {})", course.getName(), course.getCode());
        return course;
    }

    @Transactional
    public Course update(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", id));

        // Kiểm tra trùng code với khóa học khác
        var existing = courseRepository.findByCode(request.getCode());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new IllegalArgumentException("Mã khóa học '" + request.getCode() + "' đã được sử dụng bởi khóa học khác");
        }
        if (request.getMinStudents() >= request.getMaxStudents()) {
            throw new IllegalArgumentException("Sĩ số tối thiểu phải nhỏ hơn sĩ số tối đa");
        }

        course.setCode(request.getCode());
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setTotalSessions(request.getTotalSessions());
        course.setTotalLessons(request.getTotalSessions()); // giữ đồng bộ legacy
        course.setSessionsPerWeek(request.getSessionsPerWeek());
        course.setDurationWeeks(request.getDurationWeeks());
        course.setMinStudents(request.getMinStudents());
        course.setMaxStudents(request.getMaxStudents());
        course.setTuitionFee(request.getTuitionFee() != null ? request.getTuitionFee() : BigDecimal.ZERO);
        course.setRequiredSkillCode(request.getRequiredSkillCode());
        if (request.getDefaultDuration() != null) {
            course.setDefaultDuration(request.getDefaultDuration());
        }

        courseRepository.update(course);
        log.info("Đã cập nhật khóa học ID: {} ({})", id, course.getName());
        return course;
    }

    @Transactional
    public void changeStatus(Long id, CourseStatus status) {
        courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", id));
        courseRepository.changeStatus(id, status.name());
        log.info("Đã đổi trạng thái khóa học ID: {} thành {}", id, status);
    }

    @Transactional
    public void delete(Long id) {
        courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", id));
        courseRepository.deleteById(id);
        log.info("Đã xóa mềm khóa học ID: {}", id);
    }
}
