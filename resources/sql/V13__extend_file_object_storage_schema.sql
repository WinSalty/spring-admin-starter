ALTER TABLE sys_file
    ADD COLUMN storage_type VARCHAR(16) NOT NULL DEFAULT 'local' COMMENT '存储类型' AFTER file_path,
    ADD COLUMN object_key VARCHAR(500) NOT NULL DEFAULT '' COMMENT '对象存储 key' AFTER storage_type,
    ADD COLUMN file_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件访问地址' AFTER object_key,
    ADD KEY idx_sys_file_storage_type (storage_type);
