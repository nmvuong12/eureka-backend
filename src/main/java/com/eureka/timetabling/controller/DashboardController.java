package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.DashboardStatsResponse;
import com.eureka.timetabling.service.impl.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp API số liệu thống kê cho Dashboard
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bảng điều khiển (Dashboard)", description = "Cung cấp số liệu thống kê tổng quan hệ thống")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'TEACHER')")
    @Operation(summary = "Lấy số liệu thống kê tổng quan cho Dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardStats()));
    }
}
