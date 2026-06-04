package com.eureka.timetabling.notification;

import com.eureka.timetabling.domain.WebNotification;
import com.eureka.timetabling.repository.WebNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Service quản lý đăng ký SSE và đẩy thông báo đẩy Web thời gian thực */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private final WebNotificationRepository webNotificationRepository;

    // Map chứa các SseEmitter đang hoạt động, khóa là emitter, giá trị là vai trò của người dùng
    private final Map<SseEmitter, String> emitters = new ConcurrentHashMap<>();

    /** Đăng ký Emitter mới cho Client */
    public void register(SseEmitter emitter, String role) {
        emitters.put(emitter, role);
        log.info("Client đăng ký SSE thành công với vai trò: {}", role);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE Client hoàn thành kết nối");
        });

        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            log.info("SSE Client kết nối bị quá thời gian (Timeout)");
        });

        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.warn("SSE Client gặp lỗi kết nối: {}", ex.getMessage());
        });

        // Gửi tin nhắn chào mừng lúc kết nối
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Đã thiết lập kết nối thời gian thực thành công. Vai trò: " + role));
        } catch (IOException e) {
            emitters.remove(emitter);
            log.error("Không thể gửi tin nhắn INIT tới SSE client: {}", e.getMessage());
        }
    }

    /** Lưu thông báo vào CSDL và đẩy sự kiện đẩy cho các Client thuộc các Role cụ thể */
    public void saveAndBroadcast(List<String> roles, String message) {
        // 1. Lưu thông báo vào cơ sở dữ liệu cho từng vai trò nhận
        for (String role : roles) {
            try {
                WebNotification notif = WebNotification.builder()
                        .recipientRole(role)
                        .message(message)
                        .build();
                webNotificationRepository.save(notif);
            } catch (Exception e) {
                log.error("Lỗi khi lưu Web Notification vào DB: {}", e.getMessage());
            }
        }

        // 2. Broadcast qua các SSE Emitter đang hoạt động khớp Role
        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach((emitter, userRole) -> {
            if (roles.contains(userRole)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("NOTIFICATION")
                            .data(message));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                    log.error("Lỗi gửi thông báo qua SSE, xóa client khỏi map: {}", e.getMessage());
                }
            }
        });

        deadEmitters.forEach(emitters::remove);
    }
}
