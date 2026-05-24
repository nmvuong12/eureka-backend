package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.ClassPlanningLog;
import com.eureka.timetabling.domain.ClassStatus;
import com.eureka.timetabling.domain.SchoolClass;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.ClassPlanningLogRepository;
import com.eureka.timetabling.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quản lý máy trạng thái (State Machine) của Lớp học (ClassStatus)
 * và ghi nhận lịch sử thay đổi trạng thái (ClassPlanningLog).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassStateMachineService {

    private final ClassRepository classRepository;
    private final ClassPlanningLogRepository classPlanningLogRepository;

    /**
     * Chuyển đổi trạng thái lớp học có kiểm tra tính hợp lệ và ghi log.
     *
     * @param classId   ID của lớp học
     * @param newStatus Trạng thái mới muốn chuyển tới
     * @param action    Hành động kích hoạt (CREATED, STATUS_CHANGED, REBALANCED, MERGED, FORCE_OPENED, CANCELLED)
     * @param note      Ghi chú lý do chuyển trạng thái
     */
    @Transactional
    public void transition(Long classId, ClassStatus newStatus, String action, String note) {
        SchoolClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", classId));

        ClassStatus oldStatus = clazz.getStatus();
        if (oldStatus == newStatus) {
            return;
        }

        if (!isValidTransition(oldStatus, newStatus)) {
            throw new IllegalStateException("Không thể chuyển trạng thái lớp học từ " + oldStatus + " sang " + newStatus);
        }

        // Cập nhật trạng thái trong database
        classRepository.updateStatus(classId, newStatus.name());

        // Lấy username hiện tại từ SecurityContext
        String username = "Staff";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
            }
        } catch (Exception ignored) {}

        // Ghi nhận log thay đổi
        ClassPlanningLog auditLog = ClassPlanningLog.builder()
                .classId(classId)
                .action(action)
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus.name())
                .note(note)
                .createdBy(username)
                .build();

        classPlanningLogRepository.save(auditLog);
        log.info("Lớp ID {}: Chuyển trạng thái từ {} sang {} bởi hành động: {}", classId, oldStatus, newStatus, action);
    }

    /**
     * Ghi nhận audit log khi có hành động không thay đổi trạng thái chính (ví dụ REBALANCED cập nhật sĩ số).
     */
    @Transactional
    public void logAction(Long classId, String action, String note) {
        SchoolClass clazz = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", classId));

        String username = "Staff";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
            }
        } catch (Exception ignored) {}

        ClassPlanningLog auditLog = ClassPlanningLog.builder()
                .classId(classId)
                .action(action)
                .oldStatus(clazz.getStatus() != null ? clazz.getStatus().name() : null)
                .newStatus(clazz.getStatus() != null ? clazz.getStatus().name() : null)
                .note(note)
                .createdBy(username)
                .build();

        classPlanningLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<ClassPlanningLog> getLogs(Long classId) {
        classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp học", classId));
        return classPlanningLogRepository.findByClassId(classId);
    }

    /**
     * Xác định xem việc chuyển từ oldStatus -> newStatus có hợp lệ theo thiết kế không.
     */
    public boolean isValidTransition(ClassStatus oldStatus, ClassStatus newStatus) {
        if (oldStatus == null) {
            return newStatus == ClassStatus.DRAFT;
        }

        // Bất kỳ trạng thái nào chưa học (ngoại trừ Finished/Cancelled) đều có thể bị Hủy
        if (newStatus == ClassStatus.CANCELLED) {
            return oldStatus != ClassStatus.FINISHED && oldStatus != ClassStatus.CANCELLED;
        }

        return switch (oldStatus) {
            case DRAFT -> newStatus == ClassStatus.ENROLLING;
            case ENROLLING -> newStatus == ClassStatus.REBALANCING || newStatus == ClassStatus.OPEN;
            case REBALANCING -> newStatus == ClassStatus.OPEN;
            case OPEN -> newStatus == ClassStatus.STUDYING;
            case STUDYING -> newStatus == ClassStatus.FINISHED;
            case FINISHED, CANCELLED -> false; // Trạng thái cuối
            case PENDING -> newStatus == ClassStatus.ACTIVE;
            case ACTIVE -> newStatus == ClassStatus.COMPLETED;
            case COMPLETED -> false;
            default -> false;
        };
    }
}
