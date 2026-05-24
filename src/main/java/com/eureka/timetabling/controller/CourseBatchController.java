package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.CourseBatch;
import com.eureka.timetabling.domain.CourseBatchStatus;
import com.eureka.timetabling.dto.request.CourseBatchRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.service.CourseBatchService;
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
 * Controller quản lý Kế hoạch khai giảng (CourseBatch).
 */
@RestController
@RequestMapping("/course-batches")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Kế hoạch khai giảng", description = "API quản lý kế hoạch khai giảng và tuyển sinh")
public class CourseBatchController {

    private final CourseBatchService courseBatchService;

    @GetMapping
    @Operation(summary = "Danh sách kế hoạch khai giảng")
    public ResponseEntity<ApiResponse<List<CourseBatch>>> getAll(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate expectedOpeningDate) {
        List<CourseBatch> list = courseBatchService.findAll(courseId, status, expectedOpeningDate);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết kế hoạch khai giảng")
    public ResponseEntity<ApiResponse<CourseBatch>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseBatchService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo kế hoạch khai giảng mới")
    public ResponseEntity<ApiResponse<CourseBatch>> create(
            @Valid @RequestBody CourseBatchRequest request) {
        CourseBatch batch = courseBatchService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo kế hoạch khai giảng thành công", batch));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật kế hoạch khai giảng")
    public ResponseEntity<ApiResponse<CourseBatch>> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseBatchRequest request) {
        CourseBatch batch = courseBatchService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật kế hoạch khai giảng thành công", batch));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa kế hoạch khai giảng")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        courseBatchService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa kế hoạch khai giảng thành công", null));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Chuyển trạng thái kế hoạch khai giảng")
    public ResponseEntity<ApiResponse<Void>> transitionStatus(
            @PathVariable Long id,
            @RequestParam CourseBatchStatus status) {
        courseBatchService.transition(id, status);
        return ResponseEntity.ok(ApiResponse.success("Chuyển trạng thái kế hoạch khai giảng thành công", null));
    }
}
