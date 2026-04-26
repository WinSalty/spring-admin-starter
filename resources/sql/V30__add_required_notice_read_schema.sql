ALTER TABLE sys_notice
    ADD COLUMN is_required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必读公告' AFTER priority,
    ADD KEY idx_sys_notice_required_status (is_required, status);

CREATE TABLE IF NOT EXISTS sys_notice_read (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    UNIQUE KEY uk_sys_notice_read_user_notice (user_id, notice_id),
    KEY idx_sys_notice_read_notice (notice_id),
    KEY idx_sys_notice_read_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录表';
