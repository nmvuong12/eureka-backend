package com.eureka.timetabling.service;

import com.eureka.timetabling.domain.Skill;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;

    public PageResponse<Skill> search(String query, int page, int size) {
        List<Skill> content = skillRepository.search(query, page, size);
        long total = skillRepository.countSearch(query);
        return PageResponse.of(content, page, size, total);
    }

    public List<Skill> findAll() {
        return skillRepository.findAll();
    }

    public ApiResponse<Skill> findById(Long id) {
        return skillRepository.findById(id)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Kỹ năng không tồn tại"));
    }

    public ApiResponse<Long> create(Skill skill) {
        if (skillRepository.existsByCode(skill.getSkillCode())) {
            return ApiResponse.error("Mã kỹ năng đã tồn tại");
        }
        Long id = skillRepository.save(skill);
        return ApiResponse.success("Tạo kỹ năng thành công", id);
    }

    public ApiResponse<Void> update(Long id, Skill skill) {
        if (skillRepository.findById(id).isEmpty()) {
            return ApiResponse.error("Kỹ năng không tồn tại");
        }
        skill.setId(id);
        skillRepository.update(skill);
        return ApiResponse.success("Cập nhật kỹ năng thành công", null);
    }

    public ApiResponse<Void> delete(Long id) {
        int count = skillRepository.deleteById(id);
        if (count > 0) {
            return ApiResponse.success("Xóa kỹ năng thành công", null);
        }
        return ApiResponse.error("Kỹ năng không tồn tại");
    }
}
