-- =====================================================================
-- TodoList - MySQL 8.x schema
-- Quy tắc đặt tên:
--   * Tên bảng : chữ thường, số nhiều          (users, todos)
--   * Tên cột  : snake_case                    (user_id, created_date)
--   * Khóa/chỉ mục: pk_/uk_/idx_/fk_ + tên bảng
-- Chạy file này 1 lần (IntelliJ: Database tool window -> Run script,
-- hoặc: mysql -u root -p < database/schema.sql)
-- =====================================================================

DROP DATABASE IF EXISTS todolist_db;

CREATE DATABASE todolist_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE todolist_db;

-- ---------------------------------------------------------------------
-- Bảng users  <->  entity AppUser
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id      BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    email        VARCHAR(100) NOT NULL,
    password     VARCHAR(255) NOT NULL,              -- BCrypt hash
    role         VARCHAR(10)  NOT NULL,              -- ADMIN | USER
    status       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | INACTIVE
    created_date DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_users          PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT ck_users_role     CHECK (role   IN ('ADMIN', 'USER')),
    CONSTRAINT ck_users_status   CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Bảng todos  <->  entity Todo
-- ---------------------------------------------------------------------
CREATE TABLE todos (
    todo_id      BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    title        VARCHAR(200) NOT NULL,
    description  TEXT         NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING | COMPLETED
    created_date DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_date DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_todos        PRIMARY KEY (todo_id),
    CONSTRAINT ck_todos_status CHECK (status IN ('PENDING', 'COMPLETED')),
    INDEX idx_todos_user_id (user_id),
    CONSTRAINT fk_todos_users  FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE           -- xóa user -> xóa luôn todo của user đó
) ENGINE = InnoDB;

-- Tài khoản admin mặc định KHÔNG seed ở đây: DataInitializer sẽ tự tạo
-- (password được mã hóa BCrypt) ở lần chạy ứng dụng đầu tiên.
