-- ============================================================
-- V10: Rolling Scheduling Module
-- ============================================================

-- 1. Nâng cấp bảng course
ALTER TABLE course
  ADD COLUMN code               VARCHAR(50)    NULL,
  ADD COLUMN sessions_per_week  INT            NOT NULL DEFAULT 3,
  ADD COLUMN duration_weeks     INT            NOT NULL DEFAULT 8,
  ADD COLUMN min_students       INT            NOT NULL DEFAULT 10,
  ADD COLUMN max_students       INT            NOT NULL DEFAULT 25,
  ADD COLUMN tuition_fee        DECIMAL(12,0)  NOT NULL DEFAULT 0,
  ADD COLUMN required_skill_code VARCHAR(100)  NULL,
  ADD COLUMN status             ENUM('ACTIVE','INACTIVE','DISCONTINUED') NOT NULL DEFAULT 'ACTIVE';

-- Migrate: sinh code từ id cho dữ liệu cũ
UPDATE course SET code = CONCAT('COURSE_', LPAD(id, 4, '0')) WHERE code IS NULL;

-- Thêm unique constraint cho code
ALTER TABLE course ADD UNIQUE INDEX uq_course_code (code);

-- 2. Bảng Schedule Pattern
CREATE TABLE IF NOT EXISTS schedule_pattern (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  code              VARCHAR(10)  NOT NULL UNIQUE,
  study_days        VARCHAR(20)  NOT NULL COMMENT '2,4,6 = T2,T4,T6',
  slot_code         VARCHAR(5)   NOT NULL COMMENT 'C1-C5',
  slot_start        TIME         NOT NULL,
  slot_end          TIME         NOT NULL,
  sessions_per_week INT          NOT NULL DEFAULT 3,
  active            TINYINT(1)   NOT NULL DEFAULT 1,
  INDEX idx_sp_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed 19 schedule patterns
INSERT IGNORE INTO schedule_pattern (code, study_days, slot_code, slot_start, slot_end, sessions_per_week) VALUES
('P001','2,4,6','C1','08:00:00','10:00:00',3),
('P002','2,4,6','C2','10:00:00','12:00:00',3),
('P003','2,4,6','C3','14:00:00','16:00:00',3),
('P004','2,4,6','C4','17:00:00','19:00:00',3),
('P005','2,4,6','C5','19:00:00','21:00:00',3),
('P006','3,5,7','C1','08:00:00','10:00:00',3),
('P007','3,5,7','C2','10:00:00','12:00:00',3),
('P008','3,5,7','C3','14:00:00','16:00:00',3),
('P009','3,5,7','C4','17:00:00','19:00:00',3),
('P010','3,5,7','C5','19:00:00','21:00:00',3),
('P011','2,4','C4','17:00:00','19:00:00',2),
('P012','2,4','C5','19:00:00','21:00:00',2),
('P013','3,5','C4','17:00:00','19:00:00',2),
('P014','3,5','C5','19:00:00','21:00:00',2),
('P015','7,1','C1','08:00:00','10:00:00',2),
('P016','7,1','C2','10:00:00','12:00:00',2),
('P017','7,1','C3','14:00:00','16:00:00',2),
('P018','7,1','C4','17:00:00','19:00:00',2),
('P019','7,1','C5','19:00:00','21:00:00',2);

-- 3. Bảng Course Batch
CREATE TABLE IF NOT EXISTS course_batch (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id             BIGINT       NOT NULL,
  batch_name            VARCHAR(200) NOT NULL,
  enrollment_start_date DATE         NULL,
  enrollment_end_date   DATE         NULL,
  expected_opening_date DATE         NULL,
  forecast_scale        INT          NOT NULL DEFAULT 0,
  status                ENUM('PLANNING','ENROLLING','OPENED','CLOSED','CANCELLED') NOT NULL DEFAULT 'PLANNING',
  created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_cb_course (course_id),
  INDEX idx_cb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Nâng cấp bảng class
ALTER TABLE class
  ADD COLUMN class_code           VARCHAR(50)   NULL,
  ADD COLUMN batch_id             BIGINT        NULL,
  ADD COLUMN schedule_pattern_id  BIGINT        NULL,
  ADD COLUMN student_count        INT           NOT NULL DEFAULT 0,
  ADD COLUMN actual_opening_date  DATE          NULL;

-- Đổi status enum (MySQL cần MODIFY COLUMN)
ALTER TABLE class MODIFY COLUMN status ENUM('DRAFT','ENROLLING','REBALANCING','OPEN','STUDYING','FINISHED','CANCELLED','PENDING','ACTIVE','COMPLETED') NOT NULL DEFAULT 'DRAFT';

-- Sinh class_code cho dữ liệu cũ
UPDATE class SET class_code = CONCAT('CLASS_', LPAD(id, 4, '0')) WHERE class_code IS NULL;

-- 5. Bảng Audit Log
CREATE TABLE IF NOT EXISTS class_planning_log (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id   BIGINT       NOT NULL,
  action     ENUM('CREATED','STATUS_CHANGED','REBALANCED','MERGED','FORCE_OPENED','CANCELLED') NOT NULL,
  old_status VARCHAR(20)  NULL,
  new_status VARCHAR(20)  NULL,
  note       TEXT         NULL,
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(100) NULL,
  INDEX idx_cpl_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
