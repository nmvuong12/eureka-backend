-- Đảm bảo mỗi giáo viên chỉ có duy nhất 1 tài khoản đăng nhập
ALTER TABLE user_account ADD CONSTRAINT UNIQUE (teacher_id);
