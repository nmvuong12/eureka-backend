-- Tạo 1 khóa học IELTS mẫu
INSERT IGNORE INTO course (id, name, total_lessons, default_duration, description) 
VALUES (100, 'IELTS 6.5 Intensive', 10, 90, 'Khóa học cấp tốc 10 buổi');

-- Tạo 1 lớp học mẫu
INSERT IGNORE INTO class (id, course_id, name, student_size, start_date, status)
VALUES (1, 100, 'IELTS-K20', 15, CURDATE(), 'ACTIVE');

-- Tạo các buổi học cho lớp này (10 buổi)
INSERT IGNORE INTO lesson (id, class_id, lesson_index, required_skill) VALUES
(1, 1, 1, 'IELTS'),
(2, 1, 2, 'IELTS'),
(3, 1, 3, 'IELTS'),
(4, 1, 4, 'IELTS'),
(5, 1, 5, 'IELTS'),
(6, 1, 6, 'IELTS'),
(7, 1, 7, 'IELTS'),
(8, 1, 8, 'IELTS'),
(9, 1, 9, 'IELTS'),
(10, 1, 10, 'IELTS');

-- Tạo các bản ghi phân công (Assignment) trống để Solver điền vào
INSERT IGNORE INTO lesson_assignment (lesson_id) VALUES
(1), (2), (3), (4), (5), (6), (7), (8), (9), (10);
