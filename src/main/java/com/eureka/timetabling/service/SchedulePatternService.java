package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.SchedulePattern;
import com.eureka.timetabling.dto.request.SchedulePatternRequest;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.SchedulePatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quản lý Mẫu lịch học chuẩn (SchedulePattern).
 */
@Service
@RequiredArgsConstructor
public class SchedulePatternService {

    private final SchedulePatternRepository schedulePatternRepository;

    @Transactional(readOnly = true)
    public List<SchedulePattern> findAll() {
        return schedulePatternRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<SchedulePattern> findAllActive() {
        return schedulePatternRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public SchedulePattern findById(Long id) {
        return schedulePatternRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", id));
    }

    @Transactional
    public SchedulePattern createPattern(SchedulePatternRequest request) {
        String code = schedulePatternRepository.getNextAvailableCode();
        SchedulePattern pattern = SchedulePattern.builder()
                .code(code)
                .studyDays(request.getStudyDays())
                .slotCode(request.getSlotCode())
                .slotStart(request.getSlotStart())
                .slotEnd(request.getSlotEnd())
                .sessionsPerWeek(request.getSessionsPerWeek())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        
        Long id = schedulePatternRepository.save(pattern);
        pattern.setId(id);
        return pattern;
    }

    @Transactional
    public SchedulePattern updatePattern(Long id, SchedulePatternRequest request) {
        SchedulePattern pattern = findById(id);
        
        pattern.setStudyDays(request.getStudyDays());
        pattern.setSlotCode(request.getSlotCode());
        pattern.setSlotStart(request.getSlotStart());
        pattern.setSlotEnd(request.getSlotEnd());
        pattern.setSessionsPerWeek(request.getSessionsPerWeek());
        if (request.getActive() != null) {
            pattern.setActive(request.getActive());
        }
        
        schedulePatternRepository.update(pattern);
        return pattern;
    }

    @Transactional
    public void toggleActive(Long id, boolean active) {
        SchedulePattern pattern = findById(id);
        pattern.setActive(active);
        schedulePatternRepository.update(pattern);
    }

    @Transactional
    public void deletePattern(Long id) {
        SchedulePattern pattern = findById(id);
        if (schedulePatternRepository.isReferenced(id)) {
            throw new IllegalStateException("Không thể xóa mẫu lịch '" + pattern.getCode() + "' vì đang có lớp học tham chiếu sử dụng.");
        }
        schedulePatternRepository.deleteById(id);
    }
}
