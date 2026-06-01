package com.eureka.timetabling.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bộ lọc ghi nhận nhật ký (Log) cho toàn bộ yêu cầu API từ Frontend gửi tới.
 * Đăng ký ở mức lọc Servlet đầu tiên để bắt được toàn bộ request (kể cả trước khi qua Spring Security).
 */
@Slf4j
@Component
@Order(1) // Chạy đầu tiên trong chuỗi filter
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = request.getRemoteAddr();

        // Bỏ qua log các tài nguyên tĩnh hoặc docs của Swagger để tránh rác màn hình console
        if (uri.contains("/swagger-ui") || uri.contains("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String fullUri = queryString != null ? uri + "?" + queryString : uri;
        long startTime = System.currentTimeMillis();

        // Ghi log khi nhận request từ FE
        log.info("[API YÊU CẦU] >>> Method: {} | URI: {} | IP: {}", method, fullUri, clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            
            // Ghi log khi phản hồi thành công hoặc thất bại kèm theo thời gian xử lý
            if (status >= 400) {
                log.warn("[API PHẢN HỒI] <<< Method: {} | URI: {} | Status: {} | Duration: {}ms",
                        method, uri, status, duration);
            } else {
                log.info("[API PHẢN HỒI] <<< Method: {} | URI: {} | Status: {} | Duration: {}ms",
                        method, uri, status, duration);
            }
        }
    }
}
