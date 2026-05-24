-- ============================================================
-- V11: Bổ sung các cột teacher_id và room_id cho bảng class
-- ============================================================

ALTER TABLE class
  ADD COLUMN teacher_id BIGINT NULL,
  ADD COLUMN room_id    BIGINT NULL;

-- Thêm khoá ngoại an toàn
ALTER TABLE class
  ADD CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_class_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE SET NULL;
