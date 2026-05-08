-- Thêm các trường thông tin chi tiết cho tài khoản Admin/Staff
ALTER TABLE user_account 
ADD COLUMN full_name VARCHAR(150),
ADD COLUMN gender VARCHAR(10),
ADD COLUMN dob DATE,
ADD COLUMN address VARCHAR(255),
ADD COLUMN phone VARCHAR(20),
ADD COLUMN email VARCHAR(200);
