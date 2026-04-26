SET @cdk_code_encrypted_column_count = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cdk_code'
      AND COLUMN_NAME = 'encrypted_code'
);
SET @cdk_code_encrypted_sql = IF(
    @cdk_code_encrypted_column_count = 0,
    'ALTER TABLE cdk_code ADD COLUMN encrypted_code VARCHAR(512) NOT NULL DEFAULT '''' COMMENT ''加密后的明文CDK'' AFTER code_hash',
    'SELECT 1'
);
PREPARE cdk_code_encrypted_stmt FROM @cdk_code_encrypted_sql;
EXECUTE cdk_code_encrypted_stmt;
DEALLOCATE PREPARE cdk_code_encrypted_stmt;

UPDATE cdk_batch
SET status = 'active'
WHERE status IN ('draft', 'pending_approval')
  AND generated_count > 0;

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2008', 5, 'CDK管理', 'cdk_code', '/system/cdk/codes', 'KeyOutlined', 85, 'menu', 'cdk_code', 'cdk:code:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '按批次查看、复制和失效CDK'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'cdk_code');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE code = 'cdk_code';

INSERT IGNORE INTO sys_role_route (role_id, route_code) VALUES
(1, 'cdk_code');

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'cdk:code:view', '查看CDK明细'),
(1, 'cdk:code:status', '变更CDK状态');
