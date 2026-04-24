CREATE TABLE IF NOT EXISTS transaction_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    event_no VARCHAR(64) NOT NULL COMMENT '事件编号',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型',
    aggregate_no VARCHAR(64) NOT NULL COMMENT '聚合编号',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    payload JSON NOT NULL COMMENT '事件载荷',
    status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '状态',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次重试时间',
    failure_message VARCHAR(512) NOT NULL DEFAULT '' COMMENT '失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_transaction_outbox_event_no (event_no),
    KEY idx_transaction_outbox_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地事务事件表';

CREATE TABLE IF NOT EXISTS point_reconciliation_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    reconcile_no VARCHAR(64) NOT NULL COMMENT '对账编号',
    checked_accounts BIGINT NOT NULL COMMENT '检查账户数',
    different_accounts BIGINT NOT NULL COMMENT '差异账户数',
    total_available_diff BIGINT NOT NULL COMMENT '可用积分差异',
    total_frozen_diff BIGINT NOT NULL COMMENT '冻结积分差异',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    checked_at DATETIME NOT NULL COMMENT '检查时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_point_reconciliation_no (reconcile_no),
    KEY idx_point_reconciliation_checked_at (checked_at),
    KEY idx_point_reconciliation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分对账记录表';
