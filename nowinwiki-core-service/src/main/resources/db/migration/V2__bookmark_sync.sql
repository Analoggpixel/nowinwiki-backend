CREATE TABLE IF NOT EXISTS bookmark_folder (
    id BIGINT NOT NULL COMMENT 'snowflake id',
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    updated_at DATETIME(3) NOT NULL,
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_bookmark_folder_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user bookmark folders';

CREATE TABLE IF NOT EXISTS bookmark (
    id BIGINT NOT NULL COMMENT 'snowflake id',
    user_id BIGINT NOT NULL,
    folder_id BIGINT NOT NULL,
    title VARCHAR(512) NOT NULL,
    language VARCHAR(32) NOT NULL,
    bookmarked_at BIGINT NOT NULL COMMENT 'epoch millis',
    description VARCHAR(512) NULL,
    thumbnail_url VARCHAR(1024) NULL,
    updated_at DATETIME(3) NOT NULL,
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bookmark_identity (user_id, folder_id, language, title),
    KEY idx_bookmark_user_updated (user_id, updated_at),
    KEY idx_bookmark_folder (folder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user bookmarks';
