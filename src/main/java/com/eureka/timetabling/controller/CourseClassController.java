package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.Course;
import com.eureka.timetabling.domain.Lesson;
import com.eureka.timetabling.domain.SchoolClass;
import com.eureka.timetabling.dto.request.ClassRequest;
import com.eureka.timetabling.dto.request.LessonAssignmentRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.exception.ResourceNotFoundException;
import com.eureka.timetabling.repository.CourseRepository;
import com.eureka.timetabling.service.impl.ClassService;
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
 * API quản lý khóa học và lớp học
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CourseClassController {

    private final CourseRepository courseRepository;
    private final ClassService classService;

    // ===== Khóa học =====

    @GetMapping("/courses")
    @Tag(name = "Khóa học")
    @Operation(summary = "Danh sách khóa học")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success(courseRepository.findAll()));
    }

    @PostMapping("/courses")
    @Tag(name = "Khóa học")
    @Operation(summary = "Tạo khóa học mới")
    public ResponseEntity<ApiResponse<Course>> createCourse(@RequestBody Course course) {
        Long id = courseRepository.save(course);
        course.setId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo khóa học thành công", course));
    }

    // ===== Lớp học =====

    @GetMapping("/classes")
    @Tag(name = "Lớp học")
    @Operation(summary = "Danh sách lớp học")
    public ResponseEntity<ApiResponse<List<SchoolClass>>> getAllClasses() {
        return ResponseEntity.ok(ApiResponse.success(classService.findAll()));
    }

    @PostMapping("/classes")
    @Tag(name = "Lớp học")
    @Operation(summary = "Tạo lớp học và tự động sinh buổi học")
    public ResponseEntity<ApiResponse<SchoolClass>> createClass(@Valid @RequestBody ClassRequest request) {
        SchoolClass clazz = classService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo lớp học thành công", clazz));
    }

    @GetMapping("/classes/{id}/lessons")
    @Tag(name = "Lớp học")
    @Operation(summary = "Danh sách buổi học của lớp")
    public ResponseEntity<ApiResponse<List<Lesson>>> getLessons(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(classService.getLessons(id)));
    }
}
