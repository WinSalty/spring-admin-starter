CREATE TABLE IF NOT EXISTS cdk_extract_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    link_no VARCHAR(64) NOT NULL COMMENT '提取链接编号',
    code_id BIGINT NOT NULL COMMENT 'CDK ID',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    token_hash VARCHAR(128) NOT NULL COMMENT '提取 token 摘要',
    max_access_count INT NOT NULL COMMENT '最大访问次数',
    accessed_count INT NOT NULL DEFAULT 0 COMMENT '已访问次数',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    disabled_by VARCHAR(64) DEFAULT NULL COMMENT '停用人',
    disabled_at DATETIME DEFAULT NULL COMMENT '停用时间',
    remark VARCHAR(512) NOT NULL DEFAULT '' COMMENT '备注',
    last_accessed_at DATETIME DEFAULT NULL COMMENT '最近访问时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cdk_extract_link_no (link_no),
    UNIQUE KEY uk_cdk_extract_token_hash (token_hash),
    KEY idx_cdk_extract_code_status (code_id, status),
    KEY idx_cdk_extract_batch_created (batch_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK提取链接表';

CREATE TABLE IF NOT EXISTS cdk_extract_access_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    access_no VARCHAR(64) NOT NULL COMMENT '访问编号',
    link_id BIGINT DEFAULT NULL COMMENT '提取链接ID',
    code_id BIGINT DEFAULT NULL COMMENT 'CDK ID',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    result VARCHAR(32) NOT NULL COMMENT '访问结果',
    failure_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '失败码',
    failure_message VARCHAR(256) NOT NULL DEFAULT '' COMMENT '失败原因',
    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    user_agent_hash VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'UA摘要',
    browser_fingerprint VARCHAR(128) NOT NULL DEFAULT '' COMMENT '浏览器指纹摘要',
    device_snapshot JSON NULL COMMENT '设备快照',
    referer VARCHAR(512) NOT NULL DEFAULT '' COMMENT '来源页面',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_cdk_extract_access_no (access_no),
    KEY idx_cdk_extract_access_link_created (link_id, created_at),
    KEY idx_cdk_extract_access_code_created (code_id, created_at),
    KEY idx_cdk_extract_access_fingerprint (browser_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK提取访问记录表';

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'cdk:code:extract-link:create', '生成CDK提取链接'),
(1, 'cdk:code:extract-link:disable', '停用CDK提取链接'),
(1, 'cdk:code:extract-link:access-record:view', '查看CDK提取访问记录');
