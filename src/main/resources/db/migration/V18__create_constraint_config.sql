CREATE TABLE constraint_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    constraint_key VARCHAR(50) NOT NULL UNIQUE,
    constraint_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_enabled TINYINT(1) DEFAULT 1,
    weight INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO constraint_config (constraint_key, constraint_name, description, is_enabled, weight) VALUES
('teacher_room_stability', 'Giữ cố định phòng cho Giáo viên', 'Hạn chế giáo viên phải di chuyển giữa các phòng học khác nhau khi dạy các ca liên tiếp trong cùng một ngày.', 1, 30),
('teacher_max_consecutive', 'Hạn chế dạy liên tiếp không nghỉ', 'Phạt nếu xếp lịch cho một giáo viên dạy từ 3 ca liên tiếp trở lên trong ngày mà không có ca trống xen kẽ.', 1, 40),
('teacher_max_daily_load', 'Giới hạn số ca dạy tối đa / ngày', 'Giới hạn giáo viên đứng lớp không quá 3 ca (tương đương 4.5 - 6 giờ học) trong một ngày.', 1, 25),
('teacher_gap', 'Giảm thiểu khoảng trống lịch dạy', 'Tránh xếp lịch dạy bị trống lửng lơ 1 ca ở giữa cho giáo viên (dạy ca 1, nghỉ ca 2, dạy ca 3).', 1, 20),
('teacher_type_preference', 'Ưu tiên ca dạy theo hợp đồng', 'Ưu tiên giáo viên Full-time dạy ban ngày, Part-time dạy tối hoặc ngày cuối tuần.', 1, 15),
('teacher_load_balance', 'Cân bằng tải dạy giữa các Giáo viên', 'Phân bổ đều số ca dạy giữa các giáo viên cùng trình độ, tránh người bị quá tải và người rảnh rỗi.', 1, 10),
('teacher_stability', 'Ổn định Giáo viên cho lớp học', 'Ưu tiên gán duy nhất 1 giáo viên phụ trách lớp học xuyên suốt các buổi trong tuần/khóa học.', 1, 50),
('prefer_assigned_teacher', 'Ưu tiên Giáo viên chủ nhiệm lớp', 'Khi xếp lịch hoặc thay đổi, ưu tiên chọn đúng giáo viên được gán làm giáo viên chủ nhiệm chính.', 1, 35),
('prefer_exact_skill_level', 'Ưu tiên trình độ kỹ năng vừa khít', 'Tránh lãng phí giáo viên trình độ cao dạy các lớp cơ bản sơ cấp, ưu tiên gán giáo viên vừa trình độ.', 1, 15),
('room_stability', 'Cố định phòng học cho Lớp học', 'Một lớp học nên được học cố định tại một phòng duy nhất trong suốt khóa học/tuần.', 1, 30);
