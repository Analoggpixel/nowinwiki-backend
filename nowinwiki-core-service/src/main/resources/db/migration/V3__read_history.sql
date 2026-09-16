CREATE TABLE IF NOT EXISTS read_history (
    user_id BIGINT NOT NULL,
    language VARCHAR(32) NOT NULL,
    title VARCHAR(512) NOT NULL,
    viewed_at BIGINT NOT NULL COMMENT 'epoch millis',
    description VARCHAR(512) NULL,
    thumbnail_url VARCHAR(1024) NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (user_id, language, title),
    KEY idx_read_history_user_updated (user_id, updated_at),
    KEY idx_read_history_user_viewed (user_id, viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user read history';
