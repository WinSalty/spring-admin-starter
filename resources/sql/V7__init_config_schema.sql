CREATE TABLE IF NOT EXISTS sys_config_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(64) NOT NULL COMMENT '配置名称',
    code VARCHAR(64) NOT NULL COMMENT '配置编码',
    config_type VARCHAR(16) NOT NULL COMMENT '配置类型',
    value_type VARCHAR(16) NOT NULL COMMENT '值类型',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_config_record_record_code (record_code),
    UNIQUE KEY uk_sys_config_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置记录表';
