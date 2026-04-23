ALTER TABLE sys_file
    ADD COLUMN bucket_type VARCHAR(32) NOT NULL DEFAULT 'public' COMMENT 'Bucket类型：public/private/temp' AFTER storage_type,
    ADD COLUMN bucket_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '实际Bucket名称或本地逻辑空间' AFTER bucket_type,
    ADD COLUMN access_policy VARCHAR(32) NOT NULL DEFAULT 'public_read' COMMENT '访问策略：public_read/private_read' AFTER bucket_name,
    ADD KEY idx_sys_file_bucket_type (bucket_type),
    ADD KEY idx_sys_file_bucket_name (bucket_name);

UPDATE sys_file
SET bucket_type = 'public',
    access_policy = 'public_read',
    bucket_name = CASE
        WHEN storage_type = 'local' THEN 'local-public'
        WHEN storage_type = 'aliyun-oss' THEN ''
        ELSE ''
    END
WHERE deleted = 0;
