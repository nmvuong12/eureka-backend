package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.domain.LeaveRequest;
import com.eureka.timetabling.dto.request.LeaveRequestDto;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.notification.EmailNotificationService;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.LeaveRequestRepository;
import com.eureka.timetabling.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Service quản lý đơn xin nghỉ và quy trình xử lý sau phê duyệt */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final EmailNotificationService notificationService;

    @Transactional(readOnly = true)
    public List<LeaveRequest> findAll(String status) {
        return leaveRequestRepository.findAll(status);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> findByTeacherId(Long teacherId) {
        return leaveRequestRepository.findByTeacherId(teacherId);
    }

    @Transactional
    public LeaveRequest create(Long teacherId, LeaveRequestDto dto) {
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", teacherId));

        if (dto.getToDate().isBefore(dto.getFromDate())) {
            throw new BusinessException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        LeaveRequest lr = LeaveRequest.builder()
                .teacherId(teacherId)
                .fromDate(dto.getFromDate())
                .toDate(dto.getToDate())
                .reason(dto.getReason())
                .status("PENDING")
                .build();
        Long id = leaveRequestRepository.save(lr);
        lr.setId(id);
        log.info("Giáo viên {} đã nộp đơn xin nghỉ từ {} đến {}", teacherId, dto.getFromDate(), dto.getToDate());
        return lr;
    }

    @Transactional
    public LeaveRequest approve(Long id, Long reviewerId) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));

        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("Đơn này đã được xử lý rồi (trạng thái: " + lr.getStatus() + ")");
        }

        leaveRequestRepository.updateStatus(id, "APPROVED", reviewerId);
        lr.setStatus("APPROVED");

        // Xoá phân công của giáo viên trong khoảng thời gian nghỉ
        int cleared = classRepository.clearAssignmentsForTeacherInDateRange(
                lr.getTeacherId(), lr.getFromDate(), lr.getToDate());
        log.info("Đã xoá {} phân công của giáo viên {} sau khi phê duyệt nghỉ", cleared, lr.getTeacherId());

        // Gửi thông báo email
        teacherRepository.findById(lr.getTeacherId()).ifPresent(teacher ->
                notificationService.sendLeaveApprovedNotification(teacher.getEmail(), teacher.getFullName(), lr));

        return lr;
    }

    @Transactional
    public LeaveRequest reject(Long id, Long reviewerId) {
        LeaveRequest lr = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));

        if (!"PENDING".equals(lr.getStatus())) {
            throw new BusinessException("Đơn này đã được xử lý rồi");
        }

        leaveRequestRepository.updateStatus(id, "REJECTED", reviewerId);
        lr.setStatus("REJECTED");

        teacherRepository.findById(lr.getTeacherId()).ifPresent(teacher ->
                notificationService.sendLeaveRejectedNotification(teacher.getEmail(), teacher.getFullName(), lr));

        return lr;
    }
}
