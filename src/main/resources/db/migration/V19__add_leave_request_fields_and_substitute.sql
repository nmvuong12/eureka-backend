-- ============================================================
-- V19: Add Leave Request Fields, Backup Schedule Columns & FCFS Substitute Offer
-- ============================================================

-- 1. Nâng cấp bảng leave_request để lưu trữ phương thức xử lý nghỉ phép
ALTER TABLE leave_request
  ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'ALL_DAY' COMMENT 'MORNING, AFTERNOON, ALL_DAY',
  ADD COLUMN makeup_option VARCHAR(20) NOT NULL DEFAULT 'NO_MAKEUP' COMMENT 'MAKEUP, NO_MAKEUP',
  ADD COLUMN makeup_date DATE NULL,
  ADD COLUMN makeup_timeslot_id BIGINT NULL,
  ADD CONSTRAINT fk_lr_makeup_timeslot FOREIGN KEY (makeup_timeslot_id) REFERENCES timeslot(id);

-- 2. Nâng cấp bảng lesson_assignment để lưu vết lịch gốc khi có thay đổi (dạy bù, dạy thay)
ALTER TABLE lesson_assignment
  ADD COLUMN original_session_date DATE NULL,
  ADD COLUMN original_timeslot_id BIGINT NULL,
  ADD COLUMN original_room_id BIGINT NULL,
  ADD COLUMN reschedule_reason VARCHAR(255) NULL,
  ADD COLUMN leave_request_id BIGINT NULL,
  ADD CONSTRAINT fk_la_orig_timeslot FOREIGN KEY (original_timeslot_id) REFERENCES timeslot(id),
  ADD CONSTRAINT fk_la_orig_room FOREIGN KEY (original_room_id) REFERENCES room(id),
  ADD CONSTRAINT fk_la_leave_request FOREIGN KEY (leave_request_id) REFERENCES leave_request(id);

-- 3. Tạo bảng lưu trữ các lời mời dạy thay First-Come-First-Serve (FCFS) qua Token
CREATE TABLE IF NOT EXISTS substitute_offer (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  leave_request_id  BIGINT NOT NULL,
  lesson_id         BIGINT NOT NULL,
  teacher_id        BIGINT NOT NULL,
  token             VARCHAR(100) NOT NULL UNIQUE,
  status            ENUM('PENDING', 'ACCEPTED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at        DATETIME NOT NULL,
  FOREIGN KEY (leave_request_id) REFERENCES leave_request(id),
  FOREIGN KEY (lesson_id) REFERENCES lesson(id),
  FOREIGN KEY (teacher_id) REFERENCES teacher(id),
  INDEX idx_so_token (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tạo bảng lưu trữ thông báo đẩy hiển thị thời gian thực trên web
CREATE TABLE IF NOT EXISTS web_notification (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  recipient_role VARCHAR(50) NOT NULL COMMENT 'ADMIN, STAFF',
  message        VARCHAR(500) NOT NULL,
  is_read        TINYINT(1) NOT NULL DEFAULT 0,
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_wn_role (recipient_role),
  INDEX idx_wn_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
