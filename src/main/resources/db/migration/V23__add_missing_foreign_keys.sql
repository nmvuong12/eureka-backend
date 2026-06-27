-- ============================================================
-- V23: Add missing foreign keys for full data integrity
-- ============================================================

-- 1. Bảng teacher_skill
ALTER TABLE teacher_skill MODIFY COLUMN skill_code VARCHAR(50) NOT NULL;
ALTER TABLE teacher_skill
  ADD CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_ts_skill FOREIGN KEY (skill_code) REFERENCES skill(skill_code) ON DELETE CASCADE;

-- 2. Bảng teacher_unavailable
ALTER TABLE teacher_unavailable
  ADD CONSTRAINT fk_tu_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_tu_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslot(id) ON DELETE CASCADE;

-- 3. Bảng course_batch
ALTER TABLE course_batch
  ADD CONSTRAINT fk_cb_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE CASCADE;

-- 4. Bảng class
ALTER TABLE class
  ADD CONSTRAINT fk_class_course FOREIGN KEY (course_id) REFERENCES course(id) ON DELETE RESTRICT,
  ADD CONSTRAINT fk_class_batch FOREIGN KEY (batch_id) REFERENCES course_batch(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_class_schedule_pattern FOREIGN KEY (schedule_pattern_id) REFERENCES schedule_pattern(id) ON DELETE SET NULL;

-- 5. Bảng class_planning_log
ALTER TABLE class_planning_log
  ADD CONSTRAINT fk_cpl_class FOREIGN KEY (class_id) REFERENCES class(id) ON DELETE CASCADE;

-- 6. Bảng lesson
ALTER TABLE lesson
  ADD CONSTRAINT fk_lesson_class FOREIGN KEY (class_id) REFERENCES class(id) ON DELETE CASCADE;

-- 7. Bảng lesson_assignment
ALTER TABLE lesson_assignment
  ADD CONSTRAINT fk_la_lesson FOREIGN KEY (lesson_id) REFERENCES lesson(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_la_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_la_room FOREIGN KEY (room_id) REFERENCES room(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_la_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslot(id) ON DELETE SET NULL;

-- 8. Bảng user_account
ALTER TABLE user_account
  ADD CONSTRAINT fk_user_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE SET NULL;

-- 9. Bảng leave_request
ALTER TABLE leave_request
  ADD CONSTRAINT fk_lr_teacher FOREIGN KEY (teacher_id) REFERENCES teacher(id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_lr_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES user_account(id) ON DELETE SET NULL;
