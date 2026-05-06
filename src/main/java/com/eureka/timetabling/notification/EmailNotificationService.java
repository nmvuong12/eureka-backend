package com.eureka.timetabling.notification;

import com.eureka.timetabling.domain.LeaveRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service gửi email thông báo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:eureka.timetabling@gmail.com}")
    private String fromEmail;

    @Async
    public void sendLeaveApprovedNotification(String toEmail, String teacherName, LeaveRequest lr) {
        String subject = "[Eureka] Đơn xin nghỉ đã được PHÊ DUYỆT";
        String body = String.format("""
                Kính gửi %s,
                
                Đơn xin nghỉ của bạn từ ngày %s đến ngày %s đã được PHÊ DUYỆT.
                
                Các buổi dạy trong thời gian này sẽ được cập nhật lại lịch.
                Vui lòng liên hệ quản lý để biết thêm chi tiết.
                
                Trân trọng,
                Trung tâm Anh ngữ Eureka
                """, teacherName, lr.getFromDate(), lr.getToDate());
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendLeaveRejectedNotification(String toEmail, String teacherName, LeaveRequest lr) {
        String subject = "[Eureka] Đơn xin nghỉ bị TỪ CHỐI";
        String body = String.format("""
                Kính gửi %s,
                
                Đơn xin nghỉ của bạn từ ngày %s đến ngày %s đã bị TỪ CHỐI.
                
                Vui lòng liên hệ quản lý để biết thêm thông tin.
                
                Trân trọng,
                Trung tâm Anh ngữ Eureka
                """, teacherName, lr.getFromDate(), lr.getToDate());
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendScheduleChangeNotification(String toEmail, String teacherName,
                                                 String className, String oldSlot, String newSlot) {
        String subject = "[Eureka] Thông báo thay đổi lịch dạy";
        String body = String.format("""
                Kính gửi %s,
                
                Lịch dạy của bạn cho lớp "%s" đã được thay đổi:
                - Ca cũ: %s
                - Ca mới: %s
                
                Vui lòng cập nhật lịch làm việc của bạn.
                
                Trân trọng,
                Trung tâm Anh ngữ Eureka
                """, teacherName, className, oldSlot, newSlot);
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendClassReminderNotification(String toEmail, String teacherName,
                                               String className, String slotInfo) {
        String subject = "[Eureka] Nhắc nhở lịch dạy sắp tới";
        String body = String.format("""
                Kính gửi %s,
                
                Nhắc nhở: Bạn có buổi dạy lớp "%s" vào %s.
                
                Vui lòng chuẩn bị bài giảng và đến đúng giờ.
                
                Trân trọng,
                Trung tâm Anh ngữ Eureka
                """, teacherName, className, slotInfo);
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Đã gửi email đến {} - {}", to, subject);
        } catch (Exception e) {
            log.error("Lỗi gửi email đến {}: {}", to, e.getMessage());
        }
    }
}
