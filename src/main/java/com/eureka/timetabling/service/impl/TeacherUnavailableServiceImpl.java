package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.TeacherRepository;
import com.eureka.timetabling.repository.TeacherUnavailableRepository;
import com.eureka.timetabling.service.TeacherUnavailableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Triển khai chi tiết Service quản lý lịch bận cố định giáo viên */
@Service
@RequiredArgsConstructor
public class TeacherUnavailableServiceImpl implements TeacherUnavailableService {

    private final TeacherUnavailableRepository unavailableRepository;
    private final TeacherRepository teacherRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> getUnavailableTimeslots(Long teacherId) {
        // Kiểm tra xem giáo viên có tồn tại không
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", teacherId));
        return unavailableRepository.findTimeslotIdsByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public void updateUnavailableTimeslots(Long teacherId, List<Long> timeslotIds) {
        // Kiểm tra xem giáo viên có tồn tại không
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", teacherId));

        // 1. Xóa toàn bộ lịch bận cũ của giáo viên
        unavailableRepository.deleteByTeacherId(teacherId);

        // 2. Lưu hàng loạt lịch bận mới
        if (timeslotIds != null && !timeslotIds.isEmpty()) {
            unavailableRepository.batchSave(teacherId, timeslotIds);
        }
    }
}
