ALTER TABLE sys_file
    ADD COLUMN content_hash VARCHAR(64) NOT NULL DEFAULT '' COMMENT '文件内容SHA-256' AFTER file_url,
    ADD KEY idx_sys_file_content_hash (content_hash);
