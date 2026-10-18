-- =====================================================================
-- Bảng password_otps  <->  entity PasswordOtp
-- Lưu OTP (đã băm BCrypt) cho chức năng đổi mật khẩu bằng OTP.
-- File này CHỈ tạo thêm bảng mới, KHÔNG xóa/đổi gì ở bảng users, todos.
-- Chạy 1 lần trên database đang dùng (an toàn khi chạy lại nhờ IF NOT EXISTS):
--   mysql -u root -p < database/password_otps.sql
-- Quy tắc đặt tên: bảng chữ thường số nhiều, cột snake_case, pk_/idx_/fk_ + tên bảng.
-- =====================================================================

USE todolist_db;

CREATE TABLE IF NOT EXISTS password_otps (
    otp_id       BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    otp_hash     VARCHAR(255) NOT NULL,                    -- BCrypt hash của OTP, KHÔNG lưu OTP gốc
    expires_at   DATETIME(6)  NOT NULL,                    -- thời điểm OTP hết hạn
    attempts     INT          NOT NULL DEFAULT 0,          -- số lần nhập sai OTP
    created_date DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_password_otps       PRIMARY KEY (otp_id),
    CONSTRAINT ck_password_otps_attempts CHECK (attempts >= 0),
    INDEX idx_password_otps_user_id (user_id),
    CONSTRAINT fk_password_otps_users FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE           -- xóa user -> xóa luôn OTP của user đó
) ENGINE = InnoDB;
