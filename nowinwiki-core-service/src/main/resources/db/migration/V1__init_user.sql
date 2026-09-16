CREATE TABLE IF NOT EXISTS wiki_user (
    id BIGINT NOT NULL COMMENT 'snowflake id',
    phone VARCHAR(20) NOT NULL COMMENT 'phone number',
    nick_name VARCHAR(64) NOT NULL COMMENT 'display name',
    icon VARCHAR(255) NULL COMMENT 'avatar url',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wiki_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='NowInWiki user account';
