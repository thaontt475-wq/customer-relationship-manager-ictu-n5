CREATE DATABASE IF NOT EXISTS crm_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE crm_db;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(191) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_username (username),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Password reset tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_password_reset_token_hash (token_hash),
    INDEX idx_password_reset_user_id (user_id),
    INDEX idx_password_reset_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Account lock and handover decisions (CRM-30)
CREATE TABLE IF NOT EXISTS user_lock_handovers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    locked_by_user_id BIGINT NOT NULL,
    lock_reason VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_handover_target
        FOREIGN KEY (target_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_handover_recipient
        FOREIGN KEY (recipient_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_handover_locked_by
        FOREIGN KEY (locked_by_user_id)
        REFERENCES users(id),

    INDEX idx_handover_target (target_user_id),
    INDEX idx_handover_recipient (recipient_user_id),
    INDEX idx_handover_created_at (created_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;
