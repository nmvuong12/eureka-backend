package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.SchedulePattern;
import com.eureka.timetabling.dto.request.SchedulePatternRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.service.SchedulePatternService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API quản lý mẫu lịch học (SchedulePattern)
 */
@RestController
@RequestMapping("/class-planning/patterns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Mẫu lịch học", description = "API quản lý mẫu lịch học chuẩn (SchedulePattern)")
public class SchedulePatternController {

    private final SchedulePatternService schedulePatternService;

    @GetMapping("/all")
    @Operation(summary = "Lấy danh sách tất cả mẫu lịch học (cả active và inactive)")
    public ResponseEntity<ApiResponse<List<SchedulePattern>>> getAllPatterns() {
        return ResponseEntity.ok(ApiResponse.success(schedulePatternService.findAll()));
    }

    @PostMapping
    @Operation(summary = "Tạo mẫu lịch học mới")
    public ResponseEntity<ApiResponse<SchedulePattern>> createPattern(@Valid @RequestBody SchedulePatternRequest request) {
        SchedulePattern created = schedulePatternService.createPattern(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mẫu lịch học thành công", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật mẫu lịch học")
    public ResponseEntity<ApiResponse<SchedulePattern>> updatePattern(
            @PathVariable Long id,
            @Valid @RequestBody SchedulePatternRequest request) {
        SchedulePattern updated = schedulePatternService.updatePattern(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mẫu lịch học thành công", updated));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Bật/Tắt trạng thái hoạt động của mẫu lịch")
    public ResponseEntity<ApiResponse<Void>> toggleActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        schedulePatternService.toggleActive(id, active);
        String msg = active ? "Kích hoạt mẫu lịch thành công" : "Tạm khóa mẫu lịch thành công";
        return ResponseEntity.ok(ApiResponse.success(msg, null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mẫu lịch học")
    public ResponseEntity<ApiResponse<Void>> deletePattern(@PathVariable Long id) {
        schedulePatternService.deletePattern(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mẫu lịch học thành công", null));
    }
}
