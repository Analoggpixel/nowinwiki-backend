ALTER TABLE bookmark_folder
    ADD COLUMN is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 = system default folder' AFTER sort_order;

-- At most one default folder per user among non-deleted rows is enforced in app logic.
CREATE INDEX idx_bookmark_folder_user_default ON bookmark_folder (user_id, is_default);
