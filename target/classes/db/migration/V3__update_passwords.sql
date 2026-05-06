-- Cập nhật lại mật khẩu cho đúng chuẩn BCrypt cost 12
UPDATE user_account 
SET password_hash = '$2a$12$hQaOoPHDDogiFU78hnQnOOVxpWiDCrE4G1n1h1DhJUMVyN8haLn1m' 
WHERE username = 'admin';

UPDATE user_account 
SET password_hash = '$2a$12$SkMgk.2v7I9otReOxTzToOUz3yoDyHnmwKeFME46Irbygrb4k7.C.' 
WHERE role = 'TEACHER';
