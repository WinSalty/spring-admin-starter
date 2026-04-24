CREATE TABLE IF NOT EXISTS cdk_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    batch_name VARCHAR(128) NOT NULL COMMENT '批次名称',
    benefit_type VARCHAR(32) NOT NULL COMMENT '权益类型',
    benefit_config JSON NOT NULL COMMENT '权益配置',
    total_count INT NOT NULL COMMENT '总数量',
    generated_count INT NOT NULL DEFAULT 0 COMMENT '已生成数量',
    redeemed_count INT NOT NULL DEFAULT 0 COMMENT '已兑换数量',
    valid_from DATETIME NOT NULL COMMENT '生效时间',
    valid_to DATETIME NOT NULL COMMENT '失效时间',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT '风险等级',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    approved_by VARCHAR(64) DEFAULT NULL COMMENT '审批人',
    approved_at DATETIME DEFAULT NULL COMMENT '审批时间',
    export_count INT NOT NULL DEFAULT 0 COMMENT '导出次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cdk_batch_no (batch_no),
    KEY idx_cdk_batch_status (status),
    KEY idx_cdk_batch_valid_time (valid_from, valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK批次表';

CREATE TABLE IF NOT EXISTS cdk_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    code_hash VARCHAR(128) NOT NULL COMMENT 'HMAC 后码值',
    code_prefix VARCHAR(32) NOT NULL COMMENT '明文前缀',
    checksum VARCHAR(16) NOT NULL COMMENT '校验位',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    redeemed_user_id BIGINT DEFAULT NULL COMMENT '兑换用户ID',
    redeemed_at DATETIME DEFAULT NULL COMMENT '兑换时间',
    redeem_record_no VARCHAR(64) DEFAULT NULL COMMENT '兑换记录号',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cdk_code_hash (code_hash),
    KEY idx_cdk_code_batch_status (batch_id, status),
    KEY idx_cdk_code_redeemed_user (redeemed_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK码表';

CREATE TABLE IF NOT EXISTS cdk_redeem_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    redeem_no VARCHAR(64) NOT NULL COMMENT '兑换记录号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    code_id BIGINT DEFAULT NULL COMMENT 'CDK ID',
    benefit_type VARCHAR(32) NOT NULL COMMENT '权益类型',
    benefit_snapshot JSON NULL COMMENT '权益快照',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    failure_code VARCHAR(64) NOT NULL DEFAULT '' COMMENT '失败码',
    failure_message VARCHAR(256) NOT NULL DEFAULT '' COMMENT '失败原因',
    client_ip VARCHAR(64) NOT NULL DEFAULT '' COMMENT '客户端IP',
    user_agent_hash VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'UA摘要',
    trace_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '链路ID',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cdk_redeem_no (redeem_no),
    UNIQUE KEY uk_cdk_redeem_user_idempotency (user_id, idempotency_key),
    KEY idx_cdk_redeem_user_created_at (user_id, created_at),
    KEY idx_cdk_redeem_batch_status (batch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK兑换记录表';

CREATE TABLE IF NOT EXISTS cdk_export_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    exported_by VARCHAR(64) NOT NULL COMMENT '导出人',
    export_count INT NOT NULL COMMENT '导出数量',
    file_fingerprint VARCHAR(128) NOT NULL COMMENT '文件指纹',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_cdk_export_batch_id (batch_id),
    KEY idx_cdk_export_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK导出审计表';
