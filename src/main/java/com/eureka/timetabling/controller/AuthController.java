package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.request.LoginRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.LoginResponse;
import com.eureka.timetabling.service.impl.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * API xác thực người dùng
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Xác thực", description = "API đăng nhập và lấy thông tin người dùng")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập", description = "Xác thực tài khoản và trả về JWT token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Thông tin người dùng hiện tại", description = "Lấy thông tin của người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<Object>> me(Authentication auth) {
        var user = authService.getCurrentUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
