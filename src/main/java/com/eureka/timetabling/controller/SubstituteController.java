package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.SubstituteOffer;
import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.notification.EmailNotificationService;
import com.eureka.timetabling.notification.NotificationSseService;
import com.eureka.timetabling.repository.ClassRepository;
import com.eureka.timetabling.repository.SubstituteOfferRepository;
import com.eureka.timetabling.repository.TeacherRepository;
import com.eureka.timetabling.service.impl.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Controller xử lý luồng xác nhận Dạy thay FCFS thông qua Token bảo mật */
@Slf4j
@RestController
@RequestMapping("/timetable/substitute")
@RequiredArgsConstructor
@Tag(name = "Xác nhận Dạy thay (FCFS)", description = "API cho phép giáo viên nhận dạy thay qua cơ chế FCFS")
public class SubstituteController {

    private final SubstituteOfferRepository substituteOfferRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final LeaveRequestService leaveRequestService;
    private final EmailNotificationService notificationService;
    private final NotificationSseService sseService;
    private final NamedParameterJdbcTemplate jdbc;

    /** Xem chi tiết lời mời dạy thay trước khi xác nhận */
    @GetMapping("/offer")
    @Operation(summary = "Xem chi tiết lời mời dạy thay qua Token")
    public ResponseEntity<ApiResponse<SubstituteOffer>> getOfferDetails(@RequestParam String token) {
        SubstituteOffer offer = substituteOfferRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Lời mời dạy thay", 0L));
        
        // Kiểm tra hết hạn động để báo cho Client
        if ("PENDING".equals(offer.getStatus()) && offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            offer.setStatus("EXPIRED");
        }
        
        return ResponseEntity.ok(ApiResponse.success(offer));
    }

    /** Xác nhận nhận dạy thay (Độc lập phân quyền - nhận dạng qua Token bảo mật) */
    @PostMapping("/claim")
    @Transactional
    @Operation(summary = "Xác nhận dạy thay ")
    public ResponseEntity<ApiResponse<Void>> claimSubstitute(@RequestParam String token) {
        // 1. Khóa bi quan dòng dữ liệu để chống Race Condition (hai người click cùng lúc)
        String lockSql = "SELECT id, leave_request_id, status, expires_at, lesson_id, teacher_id FROM substitute_offer WHERE token = :token FOR UPDATE";
        List<SubstituteOffer> list = jdbc.query(lockSql, new MapSqlParameterSource("token", token), (rs, rowNum) -> 
            SubstituteOffer.builder()
                    .id(rs.getLong("id"))
                    .leaveRequestId(rs.getLong("leave_request_id"))
                    .status(rs.getString("status"))
                    .expiresAt(rs.getTimestamp("expires_at").toLocalDateTime())
                    .lessonId(rs.getLong("lesson_id"))
                    .teacherId(rs.getLong("teacher_id"))
                    .build()
        );

        if (list.isEmpty()) {
            throw new ResourceNotFoundException("Lời mời dạy thay", 0L);
        }

        SubstituteOffer offer = list.get(0);

        // 2. Kiểm tra xem offer đã hết hạn hoặc đã được nhận bởi người khác chưa
        if (!"PENDING".equals(offer.getStatus())) {
            throw new BusinessException("Rất tiếc! Ca dạy thay này đã có giáo viên khác nhận trước.");
        }
        if (offer.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Lời mời dạy thay này đã hết hạn hiệu lực (quá 24 giờ).");
        }

        // 3. Kiểm tra xem tiết dạy này đã có giáo viên nào khác đứng lớp chưa (Double-check)
        String checkLessonSql = "SELECT teacher_id FROM lesson_assignment WHERE lesson_id = :lessonId FOR UPDATE";
        Long currentTeacherId = jdbc.queryForObject(checkLessonSql, new MapSqlParameterSource("lessonId", offer.getLessonId()), Long.class);
        
        // Lấy giáo viên xin nghỉ phép của đơn xin nghỉ này
        String queryLeaveTeacherSql = "SELECT teacher_id FROM leave_request WHERE id = :leaveRequestId";
        Long leaveTeacherId = jdbc.queryForObject(queryLeaveTeacherSql, new MapSqlParameterSource("leaveRequestId", offer.getLeaveRequestId()), Long.class);

        if (currentTeacherId != null && !currentTeacherId.equals(leaveTeacherId)) {
            throw new BusinessException("Ca dạy thay này đã có giáo viên khác được gán trực tiếp.");
        }

        // 4. Lấy thông tin lớp học, buổi dạy và giáo viên nhận để chạy conflict check
        SubstituteOffer detailedOffer = substituteOfferRepository.findByToken(token).orElseThrow();
        Teacher substitute = teacherRepository.findById(offer.getTeacherId()).orElseThrow();

        // Kiểm tra lịch bận thực tế của giáo viên nhận (Double-check conflict)
        leaveRequestService.checkFeasibility(
                detailedOffer.getSessionDate(),
                detailedOffer.getTimeslotId(), // Sửa thành timeslotId chính xác!
                null, // Phòng check sau
                null, // Lớp check sau
                substitute.getId()
        );

        // 5. Nếu khả thi, tiến hành cập nhật phân công dạy thay
        // Cập nhật trạng thái offer hiện tại thành ACCEPTED
        substituteOfferRepository.updateStatus(offer.getId(), "ACCEPTED");
        // Hết hạn toàn bộ các offer khác của cùng lesson này
        substituteOfferRepository.expireAllOthersForLesson(offer.getLessonId(), offer.getId());

        // Cập nhật giáo viên mới vào bảng lesson_assignment, ghim lịch is_pinned = 1, sao lưu lịch gốc và gán leave_request_id
        String updateAssignmentSql = """
                UPDATE lesson_assignment 
                SET teacher_id = :teacherId, 
                    reschedule_reason = :reason, 
                    is_pinned = 1, 
                    leave_request_id = COALESCE(leave_request_id, :leaveRequestId),
                    original_session_date = COALESCE(original_session_date, session_date),
                    original_timeslot_id = COALESCE(original_timeslot_id, timeslot_id),
                    original_room_id = COALESCE(original_room_id, room_id),
                    updated_at = NOW()
                WHERE lesson_id = :lessonId
                """;
        String reason = String.format("Dạy thay: %s thay cho %s", substitute.getFullName(), detailedOffer.getOriginalTeacherName());
        jdbc.update(updateAssignmentSql, new MapSqlParameterSource()
                .addValue("teacherId", substitute.getId())
                .addValue("reason", reason)
                .addValue("lessonId", offer.getLessonId())
                .addValue("leaveRequestId", offer.getLeaveRequestId()));

        // Đã gỡ bỏ tính năng tự động phê duyệt đơn xin nghỉ tại đây. Đơn xin nghỉ sẽ chỉ được duyệt khi Admin bấm Duyệt thủ công.

        log.info("Giáo viên {} ({}) đã nhận dạy thay thành công lớp {} Buổi #{} ngày {}", 
                substitute.getFullName(), substitute.getEmail(), detailedOffer.getClassCode(), detailedOffer.getLessonIndex(), detailedOffer.getSessionDate());

        // 6. Gửi thông báo email cho các bên liên quan
        try {
            // Mail cho giáo viên nhận dạy thay
            notificationService.sendScheduleChangeNotification(
                    substitute.getEmail(), 
                    substitute.getFullName(), 
                    detailedOffer.getClassCode(), 
                    "Dạy thay", 
                    detailedOffer.getTimeslotLabel()
            );
        } catch (Exception e) {
            log.error("Lỗi gửi mail xác nhận dạy thay: {}", e.getMessage());
        }

        try {
            // Mail cho giáo viên xin nghỉ phép (giáo viên gốc)
            Teacher originalTeacher = teacherRepository.findById(leaveTeacherId).orElse(null);
            if (originalTeacher != null) {
                notificationService.sendSubstituteClaimedToOriginalTeacher(
                        originalTeacher.getEmail(),
                        originalTeacher.getFullName(),
                        substitute.getFullName(),
                        detailedOffer.getClassCode(),
                        detailedOffer.getTimeslotLabel(),
                        detailedOffer.getSessionDate() != null ? detailedOffer.getSessionDate().toString() : ""
                );
            }
        } catch (Exception e) {
            log.error("Lỗi gửi mail báo cho giáo viên xin nghỉ: {}", e.getMessage());
        }

        try {
            // Mail cho giáo vụ (STAFF) và admin (ADMIN)
            String queryAdminStaffEmailsSql = """
                    SELECT DISTINCT COALESCE(t.email, ua.email) as email
                    FROM user_account ua
                    LEFT JOIN teacher t ON ua.teacher_id = t.id
                    WHERE ua.role IN ('ADMIN', 'STAFF') AND ua.is_active = 1 AND ua.is_deleted = 0
                    """;
            List<String> adminStaffEmails = jdbc.query(queryAdminStaffEmailsSql, (rs, rowNum) -> rs.getString("email"))
                    .stream()
                    .filter(email -> email != null && !email.trim().isEmpty())
                    .toList();

            for (String adminEmail : adminStaffEmails) {
                try {
                    notificationService.sendSubstituteClaimedToAdmin(
                            adminEmail,
                            substitute.getFullName(),
                            detailedOffer.getOriginalTeacherName(),
                            detailedOffer.getClassCode(),
                            detailedOffer.getTimeslotLabel(),
                            detailedOffer.getSessionDate() != null ? detailedOffer.getSessionDate().toString() : ""
                    );
                } catch (Exception ex) {
                    log.error("Lỗi gửi mail báo cho admin/giáo vụ {}: {}", adminEmail, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Lỗi gửi mail báo cho admin/giáo vụ: {}", e.getMessage());
        }

        // 7. Đẩy thông báo đẩy SSE thời gian thực cho Admin/Staff
        try {
            String broadcastMsg = String.format("Giáo viên %s đã nhận dạy thay thành công lớp %s vào ngày %s (%s) thay cho %s.",
                    substitute.getFullName(), detailedOffer.getClassCode(), 
                    detailedOffer.getSessionDate(), detailedOffer.getTimeslotLabel(), detailedOffer.getOriginalTeacherName());
            sseService.saveAndBroadcast(List.of("ADMIN", "STAFF"), broadcastMsg);
        } catch (Exception e) {
            log.error("Lỗi broadcast SSE: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success("Xác nhận dạy thay thành công", null));
    }
}
