package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.Skill;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ApiResponse<PageResponse<Skill>> search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.success(skillService.search(query, page, size));
    }

    @GetMapping("/all")
    public ApiResponse<List<Skill>> findAll() {
        return ApiResponse.success(skillService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<Skill> findById(@PathVariable Long id) {
        return skillService.findById(id);
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody Skill skill) {
        return skillService.create(skill);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Skill skill) {
        return skillService.update(id, skill);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        return skillService.delete(id);
    }
}
