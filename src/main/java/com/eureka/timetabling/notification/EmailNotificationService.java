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

    @Async
    public void sendSubstituteOfferNotification(String toEmail, String teacherName, String classCode, int lessonIndex, String dateStr, String timeslotLabel, String claimUrl) {
        String subject = "[Eureka] Lời mời nhận DẠY THAY";
        String body = String.format("""
                Kính gửi thầy/cô %s,
                
                Trung tâm tiếng Anh Eureka trân trọng mời thầy/cô đăng ký dạy thay cho ca học sau:
                - Lớp: %s
                - Buổi thứ: %d
                - Ngày học: %s
                - Ca học: %s
                
                Đây là lời mời dạy thay tự động theo cơ chế Nhận trước - Được trước.
                Vui lòng bấm vào link dưới đây để xem chi tiết và xác nhận nhận lớp:
                %s
                
                *Lưu ý: Link này có giá trị trong vòng 24 tiếng hoặc cho đến khi có giáo viên khác nhận trước.
                
                Trân trọng,
                Eureka English Center
                """, teacherName, classCode, lessonIndex, dateStr, timeslotLabel, claimUrl);
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendSubstituteClaimedToOriginalTeacher(String toEmail, String originalTeacherName, String substituteName, String classCode, String timeslotLabel, String dateStr) {
        String subject = "[Eureka] Lớp học của bạn đã có giáo viên DẠY THAY";
        String body = String.format("""
                Kính gửi thầy/cô %s,
                
                Thông báo: Buổi dạy của thầy/cô đã được nhận dạy thay bởi giáo viên khác:
                - Lớp học: %s
                - Ngày học: %s
                - Ca học: %s
                - Giáo viên dạy thay: %s
                
                Đơn xin nghỉ phép liên quan đã được phê duyệt chính thức. Cảm ơn sự hợp tác của thầy/cô.
                
                Trân trọng,
                Eureka English Center
                """, originalTeacherName, classCode, dateStr, timeslotLabel, substituteName);
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendSubstituteClaimedToAdmin(String adminEmail, String substituteName, String originalTeacherName, String classCode, String timeslotLabel, String dateStr) {
        String subject = "[Eureka] Thông báo: Đã có giáo viên nhận DẠY THAY";
        String body = String.format("""
                Kính gửi Giáo vụ / Ban Quản trị,
                
                Hệ thống ghi nhận ca dạy thay FCFS đã được tiếp nhận thành công:
                - Lớp học: %s
                - Ngày học: %s
                - Ca học: %s
                - Giáo viên nghỉ phép: %s
                - Giáo viên nhận dạy thay: %s
                
                Lịch giảng dạy tương ứng đã được tự động cập nhật ghim cứng (is_pinned = 1) và đơn xin nghỉ đã được phê duyệt.
                
                Trân trọng,
                Hệ thống Eureka Timetabling
                """, classCode, dateStr, timeslotLabel, originalTeacherName, substituteName);
        sendEmail(adminEmail, subject, body);
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
