-- ============================================================
-- V13: Add GENERATED status to course_batch status enum
-- ============================================================

ALTER TABLE course_batch 
  MODIFY COLUMN status ENUM('PLANNING','GENERATED','ENROLLING','OPENED','CLOSED','CANCELLED') 
  NOT NULL DEFAULT 'PLANNING';
