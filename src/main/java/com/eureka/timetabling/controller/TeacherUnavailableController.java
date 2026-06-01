package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.request.TeacherUnavailableRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.service.TeacherUnavailableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản lý lịch bận cố định của Giáo viên (teacher_unavailable).
 * Hỗ trợ phân quyền chặt chẽ cho Admin, Giáo vụ và Giáo viên tự đăng ký.
 */
@Slf4j
@RestController
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Tag(name = "Lịch bận giáo viên", description = "API quản lý và đăng ký lịch bận cố định của giáo viên")
@SecurityRequirement(name = "bearerAuth")
public class TeacherUnavailableController {

    private final TeacherUnavailableService unavailableService;
    private final NamedParameterJdbcTemplate jdbc;

    /**
     * API Lấy danh sách ID ca bận cố định của giáo viên.
     * Quyền hạn: ADMIN, STAFF, TEACHER (chỉ chính mình).
     */
    @GetMapping("/{teacherId}/unavailable")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Xem danh sách ca bận của giáo viên", description = "Trả về mảng ID các ca bận cố định trong tuần của giáo viên.")
    public ResponseEntity<ApiResponse<List<Long>>> getUnavailableTimeslots(@PathVariable Long teacherId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isTeacher) {
            // Xác thực xem tài khoản đăng nhập có khớp với giáo viên được yêu cầu hay không
            String sql = "SELECT teacher_id FROM user_account WHERE username = :username AND role = 'TEACHER' AND is_active = 1";
            List<Long> matchedIds = jdbc.queryForList(sql, Map.of("username", username), Long.class);
            if (matchedIds.isEmpty() || !matchedIds.get(0).equals(teacherId)) {
                log.warn("Tài khoản '{}' cố gắng xem lịch bận của giáo viên ID: {}", username, teacherId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Bạn không có quyền xem lịch bận của giáo viên khác."));
            }
        }

        List<Long> timeslotIds = unavailableService.getUnavailableTimeslots(teacherId);
        return ResponseEntity.ok(ApiResponse.success(timeslotIds));
    }

    /**
     * API Cập nhật hàng loạt ca bận cố định của giáo viên.
     * Quyền hạn: ADMIN, STAFF, TEACHER (chỉ chính mình).
     */
    @PostMapping("/{teacherId}/unavailable")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Đăng ký/Cập nhật hàng loạt ca bận của giáo viên", description = "Thay thế toàn bộ lịch bận cố định cũ bằng danh sách mới.")
    public ResponseEntity<ApiResponse<Void>> updateUnavailableTimeslots(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherUnavailableRequest request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isTeacher = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        if (isTeacher) {
            // Xác thực xem tài khoản đăng nhập có khớp với giáo viên được yêu cầu hay không
            String sql = "SELECT teacher_id FROM user_account WHERE username = :username AND role = 'TEACHER' AND is_active = 1";
            List<Long> matchedIds = jdbc.queryForList(sql, Map.of("username", username), Long.class);
            if (matchedIds.isEmpty() || !matchedIds.get(0).equals(teacherId)) {
                log.warn("Tài khoản '{}' cố gắng cập nhật lịch bận của giáo viên ID: {}", username, teacherId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Bạn không có quyền cập nhật lịch bận của giáo viên khác."));
            }
        }

        unavailableService.updateUnavailableTimeslots(teacherId, request.getTimeslotIds());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch bận cố định thành công", null));
    }
}
