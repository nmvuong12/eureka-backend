package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.dto.request.LoginRequest;
import com.eureka.timetabling.dto.response.LoginResponse;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.repository.UserRepository;
import com.eureka.timetabling.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Service xác thực và phân quyền người dùng */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng"));

        if (!user.isActive()) {
            throw new BusinessException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.passwordHash())) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không đúng");
        }

        String token = tokenProvider.generateToken(user.username(), user.role());
        return LoginResponse.builder()
                .token(token)
                .username(user.username())
                .role(user.role())
                .teacherId(user.teacherId())
                .tokenType("Bearer")
                .build();
    }

    public UserRepository.UserRecord getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Người dùng không tồn tại"));
    }
}
