CREATE TABLE IF NOT EXISTS user_preference (
    user_id BIGINT NOT NULL,
    payload JSON NOT NULL COMMENT 'serialized UserData-like preferences',
    updated_at DATETIME(3) NOT NULL,
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user app preferences';
