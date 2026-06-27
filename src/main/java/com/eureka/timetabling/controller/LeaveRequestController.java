package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.LeaveRequest;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.Room;
import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.dto.request.LeaveRequestDto;
import com.eureka.timetabling.dto.request.SubstituteDispatchRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.TeacherRepository;
import com.eureka.timetabling.service.impl.AuthService;
import com.eureka.timetabling.service.impl.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API Quản lý đơn xin nghỉ
 */
@RestController
@RequestMapping("/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Đơn xin nghỉ", description = "Quản lý đơn xin nghỉ của giáo viên")
@SecurityRequirement(name = "bearerAuth")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final AuthService authService;
    private final TeacherRepository teacherRepository;
    private final NamedParameterJdbcTemplate jdbc;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Danh sách đơn xin nghỉ (Admin/Staff)")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getAll(
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String makeupOption,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                leaveRequestService.search(teacherId, teacherName, fromDate, toDate, makeupOption, status)));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Đơn xin nghỉ của tôi", description = "Dành cho giáo viên xem lại lịch sử đơn xin nghỉ của mình")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getMyRequests(
            Authentication auth,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String makeupOption,
            @RequestParam(required = false) String status) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(
                leaveRequestService.search(user.teacherId(), null, fromDate, toDate, makeupOption, status)));
    }

    @GetMapping("/preview-affected")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Xem trước các tiết dạy bị ảnh hưởng (Tương thích ngược)")
    public ResponseEntity<ApiResponse<List<Lesson>>> previewAffected(
            Authentication auth,
            @RequestParam String fromDate,
            @RequestParam String toDate,
            @RequestParam String sessionType) {
        var user = authService.getCurrentUser(auth.getName());
        List<Lesson> list = leaveRequestService.getAffectedLessons(
                user.teacherId(),
                LocalDate.parse(fromDate),
                LocalDate.parse(toDate),
                sessionType
        );
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/preview-affected")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Xem trước các tiết dạy bị ảnh hưởng (dùng cấu hình ngày chi tiết)")
    public ResponseEntity<ApiResponse<List<Lesson>>> previewAffectedPost(
            Authentication auth,
            @RequestBody LeaveRequestDto requestDto) {
        var user = authService.getCurrentUser(auth.getName());
        
        List<LeaveRequestDto.DayConfigDto> dayConfigs = requestDto.getDayConfigs();
        if (dayConfigs == null || dayConfigs.isEmpty()) {
            java.util.ArrayList<LeaveRequestDto.DayConfigDto> configs = new java.util.ArrayList<>();
            LocalDate current = requestDto.getFromDate();
            while (current != null && requestDto.getToDate() != null && !current.isAfter(requestDto.getToDate())) {
                LeaveRequestDto.DayConfigDto conf = new LeaveRequestDto.DayConfigDto();
                conf.setDate(current);
                conf.setSessionType(requestDto.getSessionType() != null ? requestDto.getSessionType() : "ALL_DAY");
                configs.add(conf);
                current = current.plusDays(1);
            }
            dayConfigs = configs;
        }
        
        List<Lesson> list = leaveRequestService.getAffectedLessons(user.teacherId(), dayConfigs);
        return ResponseEntity.ok(ApiResponse.success(list));
    }


    @GetMapping("/{id}/affected-lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Xem chi tiết các tiết dạy bị ảnh hưởng bởi đơn xin nghỉ")
    public ResponseEntity<ApiResponse<List<Lesson>>> getAffectedLessonsForRequest(
            @PathVariable Long id,
            @RequestParam(required = false) Long makeupRoomId,
            Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        LeaveRequest lr = leaveRequestService.findAll(null).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Đơn xin nghỉ", id));
        
        // Nếu là giáo viên, chỉ được xem đơn xin nghỉ của chính mình
        if ("TEACHER".equalsIgnoreCase(user.role()) && !lr.getTeacherId().equals(user.teacherId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem đơn xin nghỉ của giáo viên khác");
        }

        List<Lesson> list = leaveRequestService.getAffectedLessonsByRequest(
                lr.getId(), lr.getTeacherId(), lr.getFromDate(), lr.getToDate(), lr.getSessionType(), makeupRoomId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}/available-rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy danh sách các phòng học trống vào các ca/ngày dạy bù của đơn và đủ sức chứa")
    public ResponseEntity<ApiResponse<List<Room>>> getAvailableRoomsForRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.getAvailableRoomsForRequest(id)));
    }

    @GetMapping("/check-feasibility-dynamic")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Kiểm tra tính khả thi động (không cần lưu đơn)")
    public ResponseEntity<ApiResponse<String>> checkFeasibilityDynamic(
            @RequestParam LocalDate date,
            @RequestParam Long timeslotId,
            @RequestParam(required = false) Long roomId,
            @RequestParam Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long excludeLessonId,
            Authentication auth) {
        Long resolvedTeacherId = teacherId;
        if (resolvedTeacherId == null) {
            var user = authService.getCurrentUser(auth.getName());
            resolvedTeacherId = user.teacherId();
        }
        String result = leaveRequestService.assessFeasibility(date, timeslotId, roomId, classId, resolvedTeacherId, excludeLessonId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/substitute/candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Tìm giáo viên thay thế phù hợp")
    public ResponseEntity<ApiResponse<List<Teacher>>> getSubstituteCandidates(
            @RequestParam Long lessonId) {
        // Lấy thông tin ca học và kỹ năng của buổi học
        String sql = """
                SELECT l.required_skill, la.timeslot_id, la.session_date, la.teacher_id
                FROM lesson l
                INNER JOIN lesson_assignment la ON l.id = la.lesson_id
                WHERE l.id = :lessonId
                """;
        var result = jdbc.queryForMap(sql, new MapSqlParameterSource("lessonId", lessonId));
        String skill = (String) result.get("required_skill");
        Long timeslotId = (Long) result.get("timeslot_id");
        LocalDate sessionDate = result.get("session_date") != null ? ((java.sql.Date) result.get("session_date")).toLocalDate() : null;
        Long excludeTeacherId = (Long) result.get("teacher_id");

        List<Teacher> candidates = teacherRepository.findSuitableSubstitutes(
                skill, timeslotId, excludeTeacherId, sessionDate);
        return ResponseEntity.ok(ApiResponse.success(candidates));
    }

    @PostMapping("/substitute/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Gửi lời mời dạy thay FCFS hàng loạt")
    public ResponseEntity<ApiResponse<Void>> dispatchSubstituteOffers(
            @RequestBody SubstituteDispatchRequest request) {
        leaveRequestService.dispatchSubstituteOffers(
                request.getLeaveRequestId(),
                request.getLessonId(),
                request.getTeacherIds()
        );
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu dạy thay thành công", null));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Nộp đơn xin nghỉ", description = "Dành cho giáo viên nộp đơn")
    public ResponseEntity<ApiResponse<LeaveRequest>> create(
            Authentication auth, @Valid @RequestBody LeaveRequestDto request) {
        var user = authService.getCurrentUser(auth.getName());
        LeaveRequest lr = leaveRequestService.create(user.teacherId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nộp đơn xin nghỉ thành công", lr));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Chỉnh sửa đơn xin nghỉ (khi đang chờ duyệt)")
    public ResponseEntity<ApiResponse<LeaveRequest>> update(
            @PathVariable Long id, 
            Authentication auth, 
            @Valid @RequestBody LeaveRequestDto request) {
        var user = authService.getCurrentUser(auth.getName());
        LeaveRequest lr = leaveRequestService.update(id, user.teacherId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật và gửi lại đơn xin nghỉ phép thành công", lr));
    }

    @GetMapping("/{id}/check-feasibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Kiểm tra tính khả thi của các ca dạy bù trong đơn xin nghỉ")
    public ResponseEntity<ApiResponse<Void>> checkFeasibility(
            @PathVariable Long id, 
            @RequestParam Long makeupRoomId) {
        leaveRequestService.checkLeaveRequestFeasibility(id, makeupRoomId);
        return ResponseEntity.ok(ApiResponse.success("Các ca dạy bù hoàn toàn khả thi", null));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Phê duyệt đơn xin nghỉ", description = "Xóa phân công hoặc gán ca dạy bù gốc ghim cứng lịch")
    public ResponseEntity<ApiResponse<LeaveRequest>> approve(
            @PathVariable Long id, 
            @RequestParam(required = false) Long makeupRoomId, 
            Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã phê duyệt đơn xin nghỉ thành công", 
                leaveRequestService.approve(id, user.id(), makeupRoomId)));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Từ chối đơn xin nghỉ")
    public ResponseEntity<ApiResponse<LeaveRequest>> reject(
            @PathVariable Long id, Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã từ chối đơn xin nghỉ", leaveRequestService.reject(id, user.id())));
    }
}
