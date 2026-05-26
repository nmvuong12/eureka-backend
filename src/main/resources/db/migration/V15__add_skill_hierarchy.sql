-- ============================================================
-- V15: Skill Hierarchy & Graded Skills (CEFR, IELTS, TOEIC)
-- ============================================================

-- 1. Bổ sung các cột nhóm kỹ năng và thứ hạng cấp độ
ALTER TABLE skill
  ADD COLUMN skill_group VARCHAR(50) NULL,
  ADD COLUMN level_rank INT NULL;

-- 2. Thêm chỉ mục tăng tốc độ truy vấn phân cấp
ALTER TABLE skill
  ADD INDEX idx_skill_group_rank (skill_group, level_rank);

-- 3. Xóa dữ liệu cũ của bảng skill và teacher_skill để làm sạch hoàn toàn
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE teacher_skill;
TRUNCATE TABLE skill;
SET FOREIGN_KEY_CHECKS = 1;

-- 4. Seed dữ liệu kỹ năng chuẩn theo Khung tham chiếu Châu Âu (CEFR), IELTS và TOEIC
INSERT INTO skill (skill_code, skill_name, skill_group, level_rank, description) VALUES
-- Khung Châu Âu (CEFR)
('CEFR_A1', 'CEFR A1 (Beginner)', 'CEFR', 1, 'Trình độ Sơ cấp A1'),
('CEFR_A2', 'CEFR A2 (Elementary)', 'CEFR', 2, 'Trình độ Sơ cấp A2'),
('CEFR_B1', 'CEFR B1 (Intermediate)', 'CEFR', 3, 'Trình độ Trung cấp B1'),
('CEFR_B2', 'CEFR B2 (Upper Intermediate)', 'CEFR', 4, 'Trình độ Trung cấp B2'),
('CEFR_C1', 'CEFR C1 (Advanced)', 'CEFR', 5, 'Trình độ Cao cấp C1'),
('CEFR_C2', 'CEFR C2 (Proficiency)', 'CEFR', 6, 'Trình độ Cao cấp C2'),

-- IELTS
('IELTS_5.0', 'IELTS 5.0', 'IELTS', 1, 'Trình độ IELTS 5.0'),
('IELTS_5.5', 'IELTS 5.5', 'IELTS', 2, 'Trình độ IELTS 5.5'),
('IELTS_6.0', 'IELTS 6.0', 'IELTS', 3, 'Trình độ IELTS 6.0'),
('IELTS_6.5', 'IELTS 6.5', 'IELTS', 4, 'Trình độ IELTS 6.5'),
('IELTS_7.0', 'IELTS 7.0', 'IELTS', 5, 'Trình độ IELTS 7.0'),
('IELTS_7.5', 'IELTS 7.5', 'IELTS', 6, 'Trình độ IELTS 7.5'),
('IELTS_8.0', 'IELTS 8.0', 'IELTS', 7, 'Trình độ IELTS 8.0'),

-- TOEIC
('TOEIC_500', 'TOEIC 500', 'TOEIC', 1, 'Trình độ TOEIC 500'),
('TOEIC_700', 'TOEIC 700', 'TOEIC', 2, 'Trình độ TOEIC 700'),
('TOEIC_900', 'TOEIC 900', 'TOEIC', 3, 'Trình độ TOEIC 900');
