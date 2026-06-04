package com.eureka.timetabling.notification;

import com.eureka.timetabling.domain.WebNotification;
import com.eureka.timetabling.dto.response.ApiResponse;
import com.eureka.timetabling.repository.WebNotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/** Controller xử lý kết nối SSE và các API truy vấn thông báo cho Admin/Staff */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Thông báo đẩy Web (SSE)", description = "API quản lý kết nối đẩy và truy vấn thông báo Admin/Staff")
@SecurityRequirement(name = "bearerAuth")
public class NotificationSseController {

    private final NotificationSseService sseService;
    private final WebNotificationRepository webNotificationRepository;

    /**
     * Endpoint đăng ký kết nối SSE nhận thông báo đẩy thời gian thực
     * Hỗ trợ xác thực. EventSource trên trình duyệt có thể dùng thư viện để truyền Header Bearer,
     * hoặc nếu không thì Principal sẽ được trích xuất nếu JWT Filter bắt được.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Authentication authentication) {
        // Mặc định timeout kết nối là 30 phút (1800000 ms)
        SseEmitter emitter = new SseEmitter(1800000L);
        
        String role = "STAFF"; // Fallback role
        if (authentication != null) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                String authName = auth.getAuthority();
                if (authName.startsWith("ROLE_")) {
                    role = authName.replace("ROLE_", "");
                    break;
                }
            }
        }
        
        sseService.register(emitter, role);
        return emitter;
    }

    /** Lấy danh sách 20 thông báo mới nhất của vai trò hiện tại */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy danh sách thông báo mới nhất")
    public ResponseEntity<ApiResponse<List<WebNotification>>> getNotifications(Authentication authentication) {
        String role = getRole(authentication);
        List<WebNotification> list = webNotificationRepository.findByRole(role, 20);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /** Đếm số lượng thông báo chưa đọc */
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Đếm số lượng thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Integer>> getUnreadCount(Authentication authentication) {
        String role = getRole(authentication);
        int count = webNotificationRepository.countUnreadByRole(role);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /** Đánh dấu toàn bộ thông báo của vai trò hiện tại là đã đọc */
    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Đánh dấu tất cả thông báo là đã đọc")
    public ResponseEntity<ApiResponse<Void>> readAll(Authentication authentication) {
        String role = getRole(authentication);
        webNotificationRepository.markAllAsRead(role);
        return ResponseEntity.ok(ApiResponse.success("Đã đọc toàn bộ thông báo", null));
    }

    /** Đánh dấu một thông báo cụ thể là đã đọc */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Đánh dấu một thông báo cụ thể là đã đọc")
    public ResponseEntity<ApiResponse<Void>> readOne(@PathVariable Long id) {
        webNotificationRepository.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đọc thông báo", null));
    }

    private String getRole(Authentication authentication) {
        if (authentication != null) {
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                String authName = auth.getAuthority();
                if (authName.startsWith("ROLE_")) {
                    return authName.replace("ROLE_", "");
                }
            }
        }
        return "STAFF";
    }
}
