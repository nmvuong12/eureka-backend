package com.eureka.timetabling.controller;

import com.eureka.timetabling.domain.Course;
import com.eureka.timetabling.domain.CourseStatus;
import com.eureka.timetabling.dto.request.CourseRequest;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.dto.response.PageResponse;
import com.eureka.timetabling.service.CourseService;
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
 * Controller quản lý danh mục khóa học (Course Catalog).
 * Tương thích ngược hoàn toàn với hệ thống cũ.
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Khóa học", description = "API quản lý danh mục khóa học")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Tìm kiếm khóa học (Phân trang hoặc Lấy tất cả)")
    public ResponseEntity<ApiResponse<?>> getCourses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "15") Integer size) {
        if (page != null) {
            PageResponse<Course> pageResponse = courseService.search(query, status, page, size);
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
        } else {
            List<Course> list = courseService.findAll();
            return ResponseEntity.ok(ApiResponse.success(list));
        }
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy tất cả khóa học (Không phân trang)")
    public ResponseEntity<ApiResponse<List<Course>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.success(courseService.findAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết khóa học")
    public ResponseEntity<ApiResponse<Course>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Tạo khóa học mới")
    public ResponseEntity<ApiResponse<Course>> create(@Valid @RequestBody CourseRequest request) {
        Course course = courseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo khóa học thành công", course));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật khóa học")
    public ResponseEntity<ApiResponse<Course>> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        Course course = courseService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khóa học thành công", course));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Thay đổi trạng thái khóa học")
    public ResponseEntity<ApiResponse<Void>> changeStatus(
            @PathVariable Long id,
            @RequestParam CourseStatus status) {
        courseService.changeStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Đổi trạng thái khóa học thành công", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa mềm khóa học")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa khóa học thành công", null));
    }
}
