package com.eureka.timetabling.service.impl;

import com.eureka.timetabling.domain.Teacher;
import com.eureka.timetabling.dto.request.TeacherRequest;
import com.eureka.timetabling.dto.response.TeacherResponse;
import com.eureka.timetabling.exception.BusinessException;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service quản lý giáo viên
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll(String status) {
        return teacherRepository.findAll(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));
        return toResponse(teacher);
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        if (teacherRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email " + request.getEmail() + " đã được sử dụng");
        }
        Teacher teacher = Teacher.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        Long id = teacherRepository.save(teacher);
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            teacherRepository.replaceSkills(id, request.getSkills());
        }
        return findById(id);
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher existing = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));

        if (teacherRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new BusinessException("Email " + request.getEmail() + " đã được sử dụng bởi giáo viên khác");
        }

        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setPhone(request.getPhone());
        existing.setStatus(request.getStatus() != null ? request.getStatus() : existing.getStatus());
        teacherRepository.update(existing);

        if (request.getSkills() != null) {
            teacherRepository.replaceSkills(id, request.getSkills());
        }
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên", id));
        teacherRepository.deleteById(id);
    }

    private TeacherResponse toResponse(Teacher t) {
        List<String> skills = teacherRepository.findSkillsByTeacherId(t.getId());
        return TeacherResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .email(t.getEmail())
                .phone(t.getPhone())
                .status(t.getStatus())
                .skills(skills)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
