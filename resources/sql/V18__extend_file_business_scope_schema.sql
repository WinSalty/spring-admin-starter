ALTER TABLE sys_file
    ADD COLUMN biz_module VARCHAR(64) NOT NULL DEFAULT 'legacy_file' COMMENT '业务模块编码' AFTER file_url,
    ADD COLUMN biz_id VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '业务主键' AFTER biz_module,
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT '文件可见性：public/private' AFTER biz_id,
    ADD COLUMN owner_type VARCHAR(32) NOT NULL DEFAULT 'admin' COMMENT '归属对象类型：user/admin/system' AFTER visibility,
    ADD COLUMN owner_id VARCHAR(64) NOT NULL DEFAULT '0' COMMENT '归属对象ID' AFTER owner_type,
    ADD KEY idx_sys_file_biz_module_biz_id (biz_module, biz_id),
    ADD KEY idx_sys_file_visibility_owner (visibility, owner_type, owner_id);

UPDATE sys_file
SET biz_module = CASE
        WHEN bucket_type = 'private' THEN 'legacy_private_file'
        ELSE 'legacy_file'
    END,
    biz_id = '0',
    visibility = CASE
        WHEN bucket_type = 'private' THEN 'private'
        ELSE 'public'
    END,
    owner_type = CASE
        WHEN bucket_type = 'private' THEN 'admin'
        ELSE 'user'
    END,
    owner_id = '0'
WHERE deleted = 0;
