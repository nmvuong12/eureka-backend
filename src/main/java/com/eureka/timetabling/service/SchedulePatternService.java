package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.SchedulePattern;
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
    public List<SchedulePattern> findAllActive() {
        return schedulePatternRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public SchedulePattern findById(Long id) {
        return schedulePatternRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mẫu lịch học", id));
    }
}
