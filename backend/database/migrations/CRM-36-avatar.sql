-- Run against the configured CRM database before deploying CRM-36 (safe to rerun).
CREATE TABLE IF NOT EXISTS user_avatars (
    user_id BIGINT NOT NULL PRIMARY KEY,
    image_path VARCHAR(100) NOT NULL,
    thumbnail_path VARCHAR(100) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_avatar FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
