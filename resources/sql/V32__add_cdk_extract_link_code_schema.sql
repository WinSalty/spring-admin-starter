CREATE TABLE IF NOT EXISTS cdk_extract_link_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    link_id BIGINT NOT NULL COMMENT '提取链接ID',
    code_id BIGINT NOT NULL COMMENT 'CDK ID',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    sort_no INT NOT NULL COMMENT '链接内排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_cdk_extract_link_code (link_id, code_id),
    KEY idx_cdk_extract_link_code_link (link_id, sort_no),
    KEY idx_cdk_extract_link_code_code (code_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDK提取链接码明细表';

INSERT IGNORE INTO cdk_extract_link_code(link_id, code_id, batch_id, sort_no)
SELECT id, code_id, batch_id, 1
FROM cdk_extract_link;
