package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.ConstraintConfig;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.repository.ConstraintConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Cấu hình thuật toán/Ràng buộc (Constraint Config).
 */
@Slf4j
@RestController
@RequestMapping("/constraints")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cấu hình Ràng buộc", description = "API quản lý cấu hình trọng số thuật toán xếp thời khóa biểu")
public class ConstraintConfigController {

    private final ConstraintConfigRepository constraintConfigRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách cấu hình toàn bộ ràng buộc")
    public ResponseEntity<ApiResponse<List<ConstraintConfig>>> getAll() {
        List<ConstraintConfig> list = constraintConfigRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping
    @Operation(summary = "Cập nhật hàng loạt cấu hình ràng buộc")
    public ResponseEntity<ApiResponse<Void>> updateAll(
            @RequestBody List<ConstraintConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Danh sách cấu hình trống"));
        }
        for (ConstraintConfig config : configs) {
            if (config.getConstraintKey() != null) {
                constraintConfigRepository.update(config);
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Cập nhật cấu hình thuật toán thành công", null));
    }
}
