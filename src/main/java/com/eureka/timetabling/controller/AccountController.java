package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.request.AccountRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.repository.UserRepository;
import com.eureka.timetabling.service.impl.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Tài khoản", description = "Quản lý tài khoản người dùng (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "Danh sách tài khoản")
    public ResponseEntity<ApiResponse<List<UserRepository.UserRecord>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(accountService.findAll()));
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản mới")
    public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody AccountRequest request) {
        accountService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản thành công", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tài khoản")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        accountService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tài khoản thành công", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa tài khoản")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tài khoản thành công", null));
    }
}
