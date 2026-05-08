-- Migration: Thêm trường is_deleted cho toàn bộ hệ thống để thực hiện xóa mềm (Soft Delete)
-- Ngày: 2026-05-09

ALTER TABLE teacher ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE room ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE course ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE class ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE user_account ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE leave_request ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE timeslot ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE lesson ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE lesson_assignment ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0;

-- Thêm index cho is_deleted để tối ưu câu lệnh SELECT
CREATE INDEX idx_teacher_deleted ON teacher(is_deleted);
CREATE INDEX idx_room_deleted ON room(is_deleted);
CREATE INDEX idx_course_deleted ON course(is_deleted);
CREATE INDEX idx_class_deleted ON class(is_deleted);
CREATE INDEX idx_user_deleted ON user_account(is_deleted);
CREATE INDEX idx_leave_deleted ON leave_request(is_deleted);
