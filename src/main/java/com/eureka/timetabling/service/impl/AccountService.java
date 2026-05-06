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

    public List<UserRepository.UserRecord> findAll() {
        return userRepository.findAll();
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
        userRepository.save(request.getUsername(), hash, request.getRole(), request.getTeacherId());
    }

    public void update(Long id, AccountRequest request) {
        if (request.getTeacherId() != null) {
            userRepository.findByTeacherId(request.getTeacherId()).ifPresent(existing -> {
                if (!existing.id().equals(id)) {
                    throw new BusinessException("Giáo viên này đã được gán cho tài khoản khác");
                }
            });
        }
        userRepository.update(id, request.getRole(), request.isActive(), request.getTeacherId());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            userRepository.updatePassword(id, passwordEncoder.encode(request.getPassword()));
        }
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
