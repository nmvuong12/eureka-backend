-- ============================================================
-- Dữ liệu mẫu cho hệ thống xếp lịch Eureka English Center
-- ============================================================

-- Tài khoản admin (password: Admin@123)
INSERT IGNORE INTO user_account (username, password_hash, role)
VALUES ('admin', '$2a$12$LqV1lbWf7xXjWVyXbK3xWOvxFJ5TcBEVkHxGgpJa6p4r/SLBI8J2K', 'ADMIN');

-- Ca học (Timeslots)
INSERT IGNORE INTO timeslot (day_of_week, start_time, end_time, label) VALUES
('MONDAY',    '07:30', '09:00', 'Thứ 2 - Ca 1'),
('MONDAY',    '09:15', '10:45', 'Thứ 2 - Ca 2'),
('MONDAY',    '13:30', '15:00', 'Thứ 2 - Ca 3'),
('MONDAY',    '15:15', '16:45', 'Thứ 2 - Ca 4'),
('MONDAY',    '18:00', '19:30', 'Thứ 2 - Ca 5'),
('TUESDAY',   '07:30', '09:00', 'Thứ 3 - Ca 1'),
('TUESDAY',   '09:15', '10:45', 'Thứ 3 - Ca 2'),
('TUESDAY',   '13:30', '15:00', 'Thứ 3 - Ca 3'),
('TUESDAY',   '15:15', '16:45', 'Thứ 3 - Ca 4'),
('TUESDAY',   '18:00', '19:30', 'Thứ 3 - Ca 5'),
('WEDNESDAY', '07:30', '09:00', 'Thứ 4 - Ca 1'),
('WEDNESDAY', '09:15', '10:45', 'Thứ 4 - Ca 2'),
('WEDNESDAY', '13:30', '15:00', 'Thứ 4 - Ca 3'),
('WEDNESDAY', '15:15', '16:45', 'Thứ 4 - Ca 4'),
('WEDNESDAY', '18:00', '19:30', 'Thứ 4 - Ca 5'),
('THURSDAY',  '07:30', '09:00', 'Thứ 5 - Ca 1'),
('THURSDAY',  '09:15', '10:45', 'Thứ 5 - Ca 2'),
('THURSDAY',  '13:30', '15:00', 'Thứ 5 - Ca 3'),
('THURSDAY',  '15:15', '16:45', 'Thứ 5 - Ca 4'),
('THURSDAY',  '18:00', '19:30', 'Thứ 5 - Ca 5'),
('FRIDAY',    '07:30', '09:00', 'Thứ 6 - Ca 1'),
('FRIDAY',    '09:15', '10:45', 'Thứ 6 - Ca 2'),
('FRIDAY',    '13:30', '15:00', 'Thứ 6 - Ca 3'),
('FRIDAY',    '15:15', '16:45', 'Thứ 6 - Ca 4'),
('FRIDAY',    '18:00', '19:30', 'Thứ 6 - Ca 5'),
('SATURDAY',  '07:30', '09:00', 'Thứ 7 - Ca 1'),
('SATURDAY',  '09:15', '10:45', 'Thứ 7 - Ca 2'),
('SATURDAY',  '13:30', '15:00', 'Thứ 7 - Ca 3'),
('SATURDAY',  '15:15', '16:45', 'Thứ 7 - Ca 4'),
('SATURDAY',  '18:00', '19:30', 'Thứ 7 - Ca 5'),
('SUNDAY',    '07:30', '09:00', 'CN - Ca 1'),
('SUNDAY',    '09:15', '10:45', 'CN - Ca 2'),
('SUNDAY',    '13:30', '15:00', 'CN - Ca 3'),
('SUNDAY',    '15:15', '16:45', 'CN - Ca 4');

-- Phòng học
INSERT IGNORE INTO room (name, capacity, status) VALUES
('Phòng A101', 25, 'ACTIVE'),
('Phòng A102', 25, 'ACTIVE'),
('Phòng A201', 30, 'ACTIVE'),
('Phòng A202', 30, 'ACTIVE'),
('Phòng B101', 20, 'ACTIVE'),
('Phòng B102', 20, 'ACTIVE'),
('Phòng VIP-1', 10, 'ACTIVE'),
('Phòng VIP-2', 10, 'ACTIVE');

-- Giáo viên mẫu
INSERT IGNORE INTO teacher (name, email, phone, status) VALUES
('Nguyễn Thị Mai',    'mai.nguyen@eureka.edu.vn',    '0901234567', 'ACTIVE'),
('Trần Văn Hùng',     'hung.tran@eureka.edu.vn',     '0912345678', 'ACTIVE'),
('Lê Thị Hoa',        'hoa.le@eureka.edu.vn',        '0923456789', 'ACTIVE'),
('Phạm Minh Tuấn',    'tuan.pham@eureka.edu.vn',     '0934567890', 'ACTIVE'),
('Hoàng Thị Lan',     'lan.hoang@eureka.edu.vn',     '0945678901', 'ACTIVE'),
('Vũ Đức Thắng',      'thang.vu@eureka.edu.vn',      '0956789012', 'ACTIVE'),
('Đặng Thị Thu',      'thu.dang@eureka.edu.vn',      '0967890123', 'ACTIVE'),
('Bùi Văn Nam',       'nam.bui@eureka.edu.vn',       '0978901234', 'ACTIVE'),
('Ngô Thị Bích',      'bich.ngo@eureka.edu.vn',      '0989012345', 'ACTIVE'),
('Dương Minh Khoa',   'khoa.duong@eureka.edu.vn',    '0990123456', 'ACTIVE');

-- Kỹ năng giáo viên
INSERT IGNORE INTO teacher_skill (teacher_id, skill_code) VALUES
(1, 'IELTS'), (1, 'TOEIC'), (1, 'GENERAL_ENGLISH'),
(2, 'GENERAL_ENGLISH'), (2, 'KIDS_ENGLISH'), (2, 'PHONICS'),
(3, 'IELTS'), (3, 'BUSINESS_ENGLISH'),
(4, 'TOEIC'), (4, 'GENERAL_ENGLISH'),
(5, 'KIDS_ENGLISH'), (5, 'PHONICS'), (5, 'GENERAL_ENGLISH'),
(6, 'IELTS'), (6, 'TOEIC'), (6, 'BUSINESS_ENGLISH'),
(7, 'GENERAL_ENGLISH'), (7, 'CONVERSATION'),
(8, 'KIDS_ENGLISH'), (8, 'PHONICS'),
(9, 'IELTS'), (9, 'CONVERSATION'), (9, 'BUSINESS_ENGLISH'),
(10, 'TOEIC'), (10, 'GENERAL_ENGLISH'), (10, 'CONVERSATION');

-- Tài khoản giáo viên (password: Teacher@123)
INSERT IGNORE INTO user_account (username, password_hash, role, teacher_id) VALUES
('mai.nguyen',  '$2a$12$iG7uC1q7L1P9jUe5dBr6ROqrYFJf8nGEg7wGHY2HRvqG0.gBNIGnC', 'TEACHER', 1),
('hung.tran',   '$2a$12$iG7uC1q7L1P9jUe5dBr6ROqrYFJf8nGEg7wGHY2HRvqG0.gBNIGnC', 'TEACHER', 2),
('hoa.le',      '$2a$12$iG7uC1q7L1P9jUe5dBr6ROqrYFJf8nGEg7wGHY2HRvqG0.gBNIGnC', 'TEACHER', 3),
('tuan.pham',   '$2a$12$iG7uC1q7L1P9jUe5dBr6ROqrYFJf8nGEg7wGHY2HRvqG0.gBNIGnC', 'TEACHER', 4),
('lan.hoang',   '$2a$12$iG7uC1q7L1P9jUe5dBr6ROqrYFJf8nGEg7wGHY2HRvqG0.gBNIGnC', 'TEACHER', 5);

-- Khóa học mẫu
INSERT IGNORE INTO course (name, total_lessons, default_duration, description) VALUES
('IELTS Cấp tốc',          20, 90, 'Khóa học IELTS chuyên sâu 20 buổi'),
('TOEIC 700+',             24, 90, 'Luyện thi TOEIC mục tiêu 700 điểm'),
('Tiếng Anh Giao tiếp',    16, 90, 'Khóa giao tiếp thực tế hàng ngày'),
('Tiếng Anh Thiếu nhi',    30, 60, 'Chương trình dành cho trẻ em 6-12 tuổi'),
('Phonics',                20, 60, 'Phát âm chuẩn nền tảng'),
('Tiếng Anh Thương mại',   20, 90, 'Business English cho người đi làm');
