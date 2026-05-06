package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.LeaveRequest;
import com.eureka.timetabling.dto.request.LeaveRequestDto;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.service.impl.AuthService;
import com.eureka.timetabling.service.impl.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Danh sách đơn xin nghỉ (Admin/Staff)")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.findAll(status)));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Đơn xin nghỉ của tôi", description = "Dành cho giáo viên xem lại lịch sử đơn xin nghỉ của mình")
    public ResponseEntity<ApiResponse<List<LeaveRequest>>> getMyRequests(Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(leaveRequestService.findByTeacherId(user.teacherId())));
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

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Phê duyệt đơn xin nghỉ", description = "Xóa phân công của GV trong thời gian nghỉ và gửi email")
    public ResponseEntity<ApiResponse<LeaveRequest>> approve(
            @PathVariable Long id, Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Đã phê duyệt đơn xin nghỉ", leaveRequestService.approve(id, user.id())));
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
