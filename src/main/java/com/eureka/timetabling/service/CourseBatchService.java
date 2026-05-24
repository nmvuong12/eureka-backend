package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.CourseBatch;
import com.eureka.timetabling.domain.CourseBatchStatus;
import com.eureka.timetabling.domain.SchoolClass;
import com.eureka.timetabling.dto.request.CourseBatchRequest;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.CourseBatchRepository;
import com.eureka.timetabling.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eureka.timetabling.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service quản lý Kế hoạch khai giảng (CourseBatch).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseBatchService {

    private final CourseBatchRepository courseBatchRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public List<CourseBatch> findAll(Long courseId, String status, LocalDate expectedOpeningDate) {
        List<CourseBatch> list = courseBatchRepository.findAll(courseId, status, expectedOpeningDate);
        for (CourseBatch batch : list) {
            int count = classRepository.findByBatchId(batch.getId()).size();
            batch.setGeneratedClassCount(count);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public CourseBatch findById(Long id) {
        CourseBatch batch = courseBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", id));
        int count = classRepository.findByBatchId(id).size();
        batch.setGeneratedClassCount(count);
        return batch;
    }

    @Transactional
    public CourseBatch create(CourseBatchRequest request) {
        // Validate khóa học
        courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", request.getCourseId()));

        validateDates(request);

        CourseBatch batch = CourseBatch.builder()
                .courseId(request.getCourseId())
                .batchName(request.getBatchName())
                .enrollmentStartDate(request.getEnrollmentStartDate())
                .enrollmentEndDate(request.getEnrollmentEndDate())
                .expectedOpeningDate(request.getExpectedOpeningDate())
                .forecastScale(request.getForecastScale() != null ? request.getForecastScale() : 0)
                .status(CourseBatchStatus.PLANNING)
                .note(request.getNote())
                .build();

        Long id = courseBatchRepository.save(batch);
        return findById(id);
    }

    @Transactional
    public CourseBatch update(Long id, CourseBatchRequest request) {
        CourseBatch batch = courseBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", id));

        // Kiểm tra kế hoạch đã phát sinh lớp học chưa
        int generatedClasses = classRepository.findByBatchId(id).size();
        if (generatedClasses > 0) {
            throw new BusinessException("Không thể chỉnh sửa kế hoạch đã phát sinh lớp học");
        }

        // Validate khóa học
        courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Khóa học", request.getCourseId()));

        validateDates(request);

        batch.setCourseId(request.getCourseId());
        batch.setBatchName(request.getBatchName());
        batch.setEnrollmentStartDate(request.getEnrollmentStartDate());
        batch.setEnrollmentEndDate(request.getEnrollmentEndDate());
        batch.setExpectedOpeningDate(request.getExpectedOpeningDate());
        batch.setForecastScale(request.getForecastScale() != null ? request.getForecastScale() : 0);
        batch.setNote(request.getNote());

        courseBatchRepository.update(batch);
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        CourseBatch batch = courseBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", id));

        // Kiểm tra kế hoạch đã phát sinh lớp học chưa
        int generatedClasses = classRepository.findByBatchId(id).size();
        if (generatedClasses > 0) {
            throw new BusinessException("Kế hoạch đã phát sinh lớp học, không thể xóa");
        }

        courseBatchRepository.delete(id);
        log.info("Đã xóa CourseBatch ID: {}", id);
    }

    @Transactional
    public void transition(Long id, CourseBatchStatus newStatus) {
        CourseBatch batch = courseBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kế hoạch khai giảng", id));

        CourseBatchStatus currentStatus = batch.getStatus();
        if (currentStatus == newStatus) {
            return;
        }

        // Validate trạng thái chuyển đổi hợp lệ
        if (!isValidTransition(currentStatus, newStatus)) {
            throw new IllegalStateException("Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }

        courseBatchRepository.updateStatus(id, newStatus.name());
        log.info("Đã chuyển trạng thái CourseBatch ID: {} từ {} sang {}", id, currentStatus, newStatus);
    }

    private void validateDates(CourseBatchRequest request) {
        if (request.getEnrollmentStartDate() != null && request.getEnrollmentEndDate() != null) {
            if (request.getEnrollmentStartDate().isAfter(request.getEnrollmentEndDate())) {
                throw new IllegalArgumentException("Ngày bắt đầu tuyển sinh không thể sau ngày kết thúc tuyển sinh");
            }
        }
        if (request.getEnrollmentEndDate() != null && request.getExpectedOpeningDate() != null) {
            if (request.getEnrollmentEndDate().isAfter(request.getExpectedOpeningDate())) {
                throw new IllegalArgumentException("Ngày kết thúc tuyển sinh không thể sau ngày khai giảng dự kiến");
            }
        }
    }

    private boolean isValidTransition(CourseBatchStatus from, CourseBatchStatus to) {
        if (to == CourseBatchStatus.CANCELLED) {
            return from != CourseBatchStatus.CLOSED; // Closed batch cannot be cancelled
        }
        return switch (from) {
            case PLANNING -> to == CourseBatchStatus.GENERATED || to == CourseBatchStatus.ENROLLING;
            case GENERATED -> to == CourseBatchStatus.ENROLLING;
            case ENROLLING -> to == CourseBatchStatus.OPENED || to == CourseBatchStatus.CLOSED;
            case OPENED -> to == CourseBatchStatus.CLOSED;
            case CLOSED, CANCELLED -> false; // Terminal states
        };
    }
}
