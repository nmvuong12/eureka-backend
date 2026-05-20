-- ============================================================
-- Migration: V9__add_teacher_details.sql
-- Description: Nâng cấp thông tin Giáo viên và bảng Đăng ký Lịch rảnh
-- ============================================================

-- 1. Thêm các cột mới dạng cho phép NULL tạm thời để tránh xung đột dữ liệu cũ
ALTER TABLE teacher ADD COLUMN teacher_code VARCHAR(50) NULL;
ALTER TABLE teacher ADD COLUMN teacher_type ENUM('FULL_TIME', 'PART_TIME') NOT NULL DEFAULT 'FULL_TIME';

-- Đổi tên cột từ name sang full_name
ALTER TABLE teacher RENAME COLUMN name TO full_name;

ALTER TABLE teacher ADD COLUMN date_of_birth DATE NULL;
ALTER TABLE teacher ADD COLUMN gender ENUM('MALE', 'FEMALE', 'OTHER') NOT NULL DEFAULT 'OTHER';
ALTER TABLE teacher ADD COLUMN address VARCHAR(255) NULL;
ALTER TABLE teacher ADD COLUMN skills VARCHAR(500) NULL;
ALTER TABLE teacher ADD COLUMN certificate_file VARCHAR(255) NULL;
ALTER TABLE teacher ADD COLUMN profile_file VARCHAR(255) NULL;

-- Thay đổi tên cột status sang working_status và điều chỉnh kiểu dữ liệu ENUM mới
ALTER TABLE teacher RENAME COLUMN status TO working_status;
ALTER TABLE teacher MODIFY COLUMN working_status ENUM('ACTIVE', 'INACTIVE', 'ON_LEAVE') NOT NULL DEFAULT 'ACTIVE';

-- Đổi tên các trường thời gian
ALTER TABLE teacher RENAME COLUMN created_at TO created_date;
ALTER TABLE teacher RENAME COLUMN updated_at TO modified_date;

-- 2. Sinh mã giáo viên tự động cho các giáo viên hiện có trong hệ thống
SET @row_num = 0;
UPDATE teacher SET teacher_code = CONCAT('GV', LPAD(@row_num:=@row_num+1, 4, '0')) WHERE teacher_code IS NULL;

-- Thiết lập cột teacher_code là NOT NULL và UNIQUE sau khi điền dữ liệu
ALTER TABLE teacher MODIFY COLUMN teacher_code VARCHAR(50) NOT NULL UNIQUE;

-- Thêm các Index tối ưu hóa truy vấn tìm kiếm
CREATE INDEX idx_teacher_code ON teacher(teacher_code);
CREATE INDEX idx_teacher_type ON teacher(teacher_type);
CREATE INDEX idx_teacher_working_status ON teacher(working_status);

-- 3. Tạo bảng đăng ký lịch rảnh (teacher_availability) cho giáo viên PART_TIME
CREATE TABLE IF NOT EXISTS teacher_availability (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id   BIGINT NOT NULL,
    day_of_week  ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    CONSTRAINT fk_availability_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
    INDEX idx_ta_teacher (teacher_id),
    INDEX idx_ta_day (day_of_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
