package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.dto.request.AccountRequest;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public com.eureka.timetabling.dto.response.PageResponse<UserRepository.UserRecord> search(String username, String role, Boolean isActive, String fullName, String email, int page, int size) {
        List<UserRepository.UserRecord> data = userRepository.search(username, role, isActive, fullName, email, page, size);
        long total = userRepository.countSearch(username, role, isActive, fullName, email);
        return com.eureka.timetabling.dto.response.PageResponse.of(data, page, size, total);
    }

    public void create(AccountRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("Tên đăng nhập đã tồn tại");
        }
        if (request.getTeacherId() != null) {
            if (userRepository.findByTeacherId(request.getTeacherId()).isPresent()) {
                throw new BusinessException("Giáo viên này đã có tài khoản trên hệ thống");
            }
        }
        String hash = passwordEncoder.encode(request.getPassword());
        userRepository.save(request.getUsername(), hash, request.getRole(), request.getTeacherId(),
                request.getFullName(), request.getGender(), request.getDob(),
                request.getAddress(), request.getPhone(), request.getEmail());
    }

    public void update(Long id, AccountRequest request) {
        if (request.getTeacherId() != null) {
            userRepository.findByTeacherId(request.getTeacherId()).ifPresent(existing -> {
                if (!existing.id().equals(id)) {
                    throw new BusinessException("Giáo viên này đã được gán cho tài khoản khác");
                }
            });
        }
        userRepository.update(id, request.getRole(), request.isActive(), request.getTeacherId(),
                request.getFullName(), request.getGender(), request.getDob(),
                request.getAddress(), request.getPhone(), request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            userRepository.updatePassword(id, passwordEncoder.encode(request.getPassword()));
        }
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
