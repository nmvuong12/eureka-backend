-- ============================================================
-- V12: Thay đổi cột start_date trong bảng class thành NULLable
-- Phục vụ cho mô hình Rolling Scheduling (lớp DRAFT chưa có ngày khai giảng)
-- ============================================================

ALTER TABLE class MODIFY COLUMN start_date DATE NULL;
