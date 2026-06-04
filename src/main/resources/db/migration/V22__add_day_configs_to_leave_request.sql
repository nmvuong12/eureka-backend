-- ============================================================
-- V22: Add day_configs column to leave_request table
-- ============================================================

ALTER TABLE leave_request
  ADD COLUMN day_configs TEXT NULL COMMENT 'JSON list of per-day configs (date, sessionType)';

