INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2001', (SELECT id FROM sys_menu WHERE code = 'account' LIMIT 1), '积分钱包', 'points_wallet', '/points/wallet', 'WalletOutlined', 20, 'menu', 'points_wallet', 'points:wallet:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '用户积分钱包入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'points_wallet');

UPDATE sys_menu
SET parent_id = (SELECT account_menu.id FROM (SELECT id FROM sys_menu WHERE code = 'account' LIMIT 1) account_menu),
    order_no = 20
WHERE code = 'points_wallet'
  AND EXISTS (SELECT 1 FROM (SELECT id FROM sys_menu WHERE code = 'account' LIMIT 1) account_menu);

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2002', 5, 'CDK批次', 'cdk_batch', '/system/cdk/batches', 'GiftOutlined', 80, 'menu', 'cdk_batch', 'cdk:batch:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', 'CDK批次管理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'cdk_batch');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2003', 5, 'CDK兑换记录', 'cdk_redeem_record', '/system/cdk/redeem-records', 'FileSearchOutlined', 90, 'menu', 'cdk_redeem_record', 'cdk:redeem:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', 'CDK兑换记录查询入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'cdk_redeem_record');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2004', 5, '积分审计', 'points_admin_audit', '/system/points/audit', 'AuditOutlined', 100, 'menu', 'points_admin_ledger', 'points:ledger:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '积分账户、流水和对账审计入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'points_admin_audit');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE code IN ('points_wallet', 'cdk_batch', 'cdk_redeem_record', 'points_admin_audit');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE code = 'points_wallet';

INSERT IGNORE INTO sys_role_route (role_id, route_code) VALUES
(1, 'points_wallet'), (1, 'points_admin_account'), (1, 'points_admin_ledger'), (1, 'cdk_batch'), (1, 'cdk_redeem_record'),
(2, 'points_wallet');

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'cdk:batch:create', '创建CDK批次'),
(1, 'cdk:batch:approve', '审批CDK批次'),
(1, 'cdk:batch:export', '导出CDK批次'),
(1, 'cdk:batch:pause', '暂停CDK批次'),
(1, 'cdk:batch:void', '作废CDK批次'),
(1, 'points:adjust:apply', '申请积分调整'),
(1, 'points:adjust:approve', '审批积分调整'),
(1, 'points:ledger:view', '查看积分流水');
