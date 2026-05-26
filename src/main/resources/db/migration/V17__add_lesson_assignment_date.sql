-- ============================================================
-- V17: Add Session Date to Lesson Assignment for Dispatch Module
-- ============================================================

-- Bổ sung cột ngày dương lịch cụ thể cho từng buổi học
ALTER TABLE lesson_assignment
  ADD COLUMN session_date DATE NULL AFTER timeslot_id,
  ADD INDEX idx_la_session_date (session_date);
