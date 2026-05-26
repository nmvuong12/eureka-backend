-- ============================================================
-- V16: Align Timeslot Hours with Schedule Pattern Hours
-- ============================================================

-- Cập nhật giờ của 5 ca học trong bảng timeslot trùng khớp hoàn toàn với 120 phút của Schedule Pattern

-- Ca 1: 07:30 -> 08:00 - 10:00
UPDATE timeslot 
SET start_time = '08:00:00', end_time = '10:00:00' 
WHERE start_time = '07:30:00' OR start_time = '07:30';

-- Ca 2: 09:15 -> 10:00 - 12:00
UPDATE timeslot 
SET start_time = '10:00:00', end_time = '12:00:00' 
WHERE start_time = '09:15:00' OR start_time = '09:15';

-- Ca 3: 13:30 -> 14:00 - 16:00
UPDATE timeslot 
SET start_time = '14:00:00', end_time = '16:00:00' 
WHERE start_time = '13:30:00' OR start_time = '13:30';

-- Ca 4: 15:15 -> 17:00 - 19:00
UPDATE timeslot 
SET start_time = '17:00:00', end_time = '19:00:00' 
WHERE start_time = '15:15:00' OR start_time = '15:15';

-- Ca 5: 18:00 -> 19:00 - 21:00
UPDATE timeslot 
SET start_time = '19:00:00', end_time = '21:00:00' 
WHERE start_time = '18:00:00' OR start_time = '18:00';
