-- ============================================================
-- V20: Add leave request lesson options for individual affected lessons
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_request_lesson (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  leave_request_id   BIGINT NOT NULL,
  lesson_id          BIGINT NOT NULL,
  makeup_option      VARCHAR(20) NOT NULL DEFAULT 'NO_MAKEUP' COMMENT 'MAKEUP, NO_MAKEUP',
  makeup_date        DATE NULL,
  makeup_timeslot_id BIGINT NULL,
  FOREIGN KEY (leave_request_id) REFERENCES leave_request(id) ON DELETE CASCADE,
  FOREIGN KEY (lesson_id) REFERENCES lesson(id),
  FOREIGN KEY (makeup_timeslot_id) REFERENCES timeslot(id),
  UNIQUE KEY uq_lr_lesson (leave_request_id, lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
