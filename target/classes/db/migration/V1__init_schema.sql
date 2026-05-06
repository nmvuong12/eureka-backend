-- ============================================================
-- Eureka English Center - Timetabling System
-- Database Schema V1 - BCNF Normalized
-- ============================================================

-- Bảng Giáo viên
CREATE TABLE IF NOT EXISTS teacher (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    email       VARCHAR(200) NOT NULL UNIQUE,
    phone       VARCHAR(20),
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher_status (status),
    INDEX idx_teacher_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Kỹ năng giáo viên
CREATE TABLE IF NOT EXISTS teacher_skill (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  BIGINT NOT NULL,
    skill_code  VARCHAR(100) NOT NULL,
    UNIQUE KEY uq_teacher_skill (teacher_id, skill_code),
    INDEX idx_ts_teacher (teacher_id),
    INDEX idx_ts_skill (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Ca học (Timeslot)
CREATE TABLE IF NOT EXISTS timeslot (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    day_of_week ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    label       VARCHAR(100),
    UNIQUE KEY uq_timeslot (day_of_week, start_time, end_time),
    INDEX idx_timeslot_day (day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Lịch bận của giáo viên
CREATE TABLE IF NOT EXISTS teacher_unavailable (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  BIGINT NOT NULL,
    timeslot_id BIGINT NOT NULL,
    reason      VARCHAR(255),
    UNIQUE KEY uq_teacher_unavail (teacher_id, timeslot_id),
    INDEX idx_tu_teacher (teacher_id),
    INDEX idx_tu_timeslot (timeslot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Phòng học
CREATE TABLE IF NOT EXISTS room (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    capacity    INT NOT NULL DEFAULT 30,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_room_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Khóa học
CREATE TABLE IF NOT EXISTS course (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    total_lessons    INT NOT NULL DEFAULT 20,
    default_duration INT NOT NULL DEFAULT 90,
    description      TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Lớp học
CREATE TABLE IF NOT EXISTS class (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT NOT NULL,
    name         VARCHAR(200) NOT NULL,
    student_size INT NOT NULL DEFAULT 20,
    start_date   DATE NOT NULL,
    status       ENUM('PENDING','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_class_course (course_id),
    INDEX idx_class_status (status),
    INDEX idx_class_start (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Buổi học (Lesson)
CREATE TABLE IF NOT EXISTS lesson (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id       BIGINT NOT NULL,
    lesson_index   INT NOT NULL,
    required_skill VARCHAR(100) NOT NULL,
    UNIQUE KEY uq_lesson (class_id, lesson_index),
    INDEX idx_lesson_class (class_id),
    INDEX idx_lesson_skill (required_skill)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Phân công buổi học (LessonAssignment - Planning Entity)
CREATE TABLE IF NOT EXISTS lesson_assignment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id   BIGINT NOT NULL UNIQUE,
    teacher_id  BIGINT,
    room_id     BIGINT,
    timeslot_id BIGINT,
    is_pinned   TINYINT(1) NOT NULL DEFAULT 0,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_la_teacher (teacher_id),
    INDEX idx_la_room (room_id),
    INDEX idx_la_timeslot (timeslot_id),
    INDEX idx_la_lesson (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Tài khoản người dùng
CREATE TABLE IF NOT EXISTS user_account (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','STAFF','TEACHER') NOT NULL DEFAULT 'STAFF',
    teacher_id    BIGINT,
    is_active     TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_role (role),
    INDEX idx_user_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Đơn xin nghỉ
CREATE TABLE IF NOT EXISTS leave_request (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  BIGINT NOT NULL,
    from_date   DATE NOT NULL,
    to_date     DATE NOT NULL,
    reason      TEXT,
    status      ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT,
    reviewed_at DATETIME,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_lr_teacher (teacher_id),
    INDEX idx_lr_status (status),
    INDEX idx_lr_dates (from_date, to_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Nhật ký thông báo
CREATE TABLE IF NOT EXISTS notification_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient    VARCHAR(200) NOT NULL,
    subject      VARCHAR(300) NOT NULL,
    body         TEXT,
    channel      ENUM('EMAIL') NOT NULL DEFAULT 'EMAIL',
    status       ENUM('PENDING','SENT','FAILED') NOT NULL DEFAULT 'PENDING',
    event_type   VARCHAR(100),
    reference_id BIGINT,
    sent_at      DATETIME,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_nl_status (status),
    INDEX idx_nl_event (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
