ALTER TABLE cdk_batch
    ADD COLUMN second_approved_by VARCHAR(64) DEFAULT NULL COMMENT '二次复核人' AFTER approved_at,
    ADD COLUMN second_approved_at DATETIME DEFAULT NULL COMMENT '二次复核时间' AFTER second_approved_by;

CREATE TABLE IF NOT EXISTS risk_alert (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    alert_no VARCHAR(64) NOT NULL COMMENT '告警编号',
    alert_type VARCHAR(64) NOT NULL COMMENT '告警类型',
    risk_level VARCHAR(32) NOT NULL COMMENT '风险等级',
    subject_type VARCHAR(64) NOT NULL COMMENT '对象类型',
    subject_no VARCHAR(128) NOT NULL COMMENT '对象编号',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    status VARCHAR(32) NOT NULL DEFAULT 'open' COMMENT '状态',
    detail_snapshot JSON NULL COMMENT '告警详情',
    handled_by VARCHAR(64) DEFAULT NULL COMMENT '处理人',
    handled_at DATETIME DEFAULT NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_risk_alert_no (alert_no),
    KEY idx_risk_alert_status_created_at (status, created_at),
    KEY idx_risk_alert_subject (subject_type, subject_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险告警表';

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2007', 5, '风控告警', 'risk_alert_admin', '/system/risk-alerts', 'WarningOutlined', 110, 'menu', 'risk_alert_admin', 'risk:alert:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '异常兑换和高价值操作风控告警入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'risk_alert_admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE code = 'risk_alert_admin';

INSERT IGNORE INTO sys_role_route (role_id, route_code) VALUES
(1, 'risk_alert_admin');

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'cdk:batch:second-approve', '二次复核CDK批次'),
(1, 'risk:alert:view', '查看风控告警');
