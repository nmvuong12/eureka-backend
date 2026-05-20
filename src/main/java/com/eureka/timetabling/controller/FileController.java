package com.eureka.timetabling.controller;

import com.eureka.timetabling.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Controller chịu trách nhiệm xử lý upload và tải tập tin hồ sơ/chứng chỉ giáo viên.
 */
@RestController
@RequestMapping("/files")
@Tag(name = "Tải file", description = "API Hỗ trợ upload và download chứng chỉ, hồ sơ giáo viên")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";

    /**
     * Tải lên một tập tin và lưu trữ cục bộ
     */
    @PostMapping("/upload")
    @Operation(summary = "Tải tập tin lên máy chủ (Hồ sơ, chứng chỉ...)")
    public ApiResponse<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error("Vui lòng chọn tập tin để tải lên");
        }

        try {
            // Tạo thư mục lưu trữ nếu chưa có
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Tạo tên file ngẫu nhiên duy nhất để tránh trùng lặp
            String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            // Copy file vào thư mục lưu trữ
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Trả về tên file độc bản để Frontend lưu lại trong cơ sở dữ liệu
            return ApiResponse.success("Tải lên tập tin thành công", uniqueFileName);
        } catch (IOException e) {
            return ApiResponse.error("Không thể tải lên tập tin: " + e.getMessage());
        }
    }

    /**
     * Tải xuống hoặc xem file đã lưu
     */
    @GetMapping("/download/{filename:.+}")
    @Operation(summary = "Xem hoặc tải về tập tin")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
