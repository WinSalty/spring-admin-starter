CREATE TABLE IF NOT EXISTS biz_query_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '名称',
    code VARCHAR(60) NOT NULL COMMENT '业务编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
    call_count BIGINT NOT NULL DEFAULT 0 COMMENT '调用次数',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_query_record_record_code (record_code),
    UNIQUE KEY uk_biz_query_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询配置表';
