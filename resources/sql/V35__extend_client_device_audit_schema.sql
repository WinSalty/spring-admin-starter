ALTER TABLE credential_extract_access_record
    ADD COLUMN user_agent TEXT COMMENT '原始User-Agent' AFTER client_ip,
    ADD COLUMN browser VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器名称' AFTER user_agent_hash,
    ADD COLUMN browser_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器版本' AFTER browser,
    ADD COLUMN os_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统名称' AFTER browser_version,
    ADD COLUMN os_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统版本' AFTER os_name,
    ADD COLUMN device_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '设备类型' AFTER os_version,
    ADD COLUMN device_brand VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备品牌' AFTER device_type;

ALTER TABLE sys_log_record
    ADD COLUMN user_agent TEXT COMMENT '原始User-Agent' AFTER device_info,
    ADD COLUMN browser VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器名称' AFTER user_agent,
    ADD COLUMN browser_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器版本' AFTER browser,
    ADD COLUMN os_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统名称' AFTER browser_version,
    ADD COLUMN os_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统版本' AFTER os_name,
    ADD COLUMN device_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '设备类型' AFTER os_version,
    ADD COLUMN device_brand VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备品牌' AFTER device_type;

ALTER TABLE sys_log_archive
    ADD COLUMN user_agent TEXT COMMENT '原始User-Agent' AFTER device_info,
    ADD COLUMN browser VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器名称' AFTER user_agent,
    ADD COLUMN browser_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '浏览器版本' AFTER browser,
    ADD COLUMN os_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统名称' AFTER browser_version,
    ADD COLUMN os_version VARCHAR(64) NOT NULL DEFAULT '' COMMENT '操作系统版本' AFTER os_name,
    ADD COLUMN device_type VARCHAR(32) NOT NULL DEFAULT '' COMMENT '设备类型' AFTER os_version,
    ADD COLUMN device_brand VARCHAR(64) NOT NULL DEFAULT '' COMMENT '设备品牌' AFTER device_type;

UPDATE sys_log_record
SET user_agent = device_info
WHERE (user_agent IS NULL OR user_agent = '')
  AND device_info <> '';

UPDATE sys_log_archive
SET user_agent = device_info
WHERE (user_agent IS NULL OR user_agent = '')
  AND device_info <> '';
