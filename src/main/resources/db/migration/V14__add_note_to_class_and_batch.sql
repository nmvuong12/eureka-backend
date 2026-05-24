-- ============================================================
-- V14: Add note column to class and course_batch tables
-- ============================================================

ALTER TABLE course_batch ADD COLUMN note VARCHAR(500) NULL;
ALTER TABLE class ADD COLUMN note VARCHAR(500) NULL;
