CREATE TABLE skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_code VARCHAR(50) NOT NULL UNIQUE,
    skill_name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_deleted TINYINT(1) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skill_deleted (is_deleted),
    INDEX idx_skill_code (skill_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert some sample skills
INSERT INTO skill (skill_code, skill_name, description) VALUES
('IELTS', 'IELTS Certificate', 'English proficiency for international education'),
('TOEIC', 'TOEIC Certificate', 'English for international communication'),
('JLPT_N1', 'JLPT N1', 'Japanese language proficiency level 1'),
('JLPT_N2', 'JLPT N2', 'Japanese language proficiency level 2'),
('TESOL', 'TESOL', 'Teaching English to Speakers of Other Languages'),
('PYTHON', 'Python Programming', 'General purpose programming language');
