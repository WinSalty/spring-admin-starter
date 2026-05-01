INSERT INTO credential_category (category_code, category_name, fulfillment_type, generation_mode, payload_schema, import_config, extract_policy, status)
SELECT 'POINTS_CDK',
       '系统积分兑换码',
       'POINTS_REDEEM',
       'SYSTEM_GENERATED',
       '{"points":{"type":"number","min":1,"required":true}}',
       '{"deduplicateInBatch":true,"deduplicateGlobal":true,"caseSensitive":false}',
       '{"allowExtract":true,"consumeOnFirstExtract":false,"allowCopyUrl":true}',
       'active'
WHERE NOT EXISTS (SELECT 1 FROM credential_category WHERE category_code = 'POINTS_CDK');

INSERT INTO credential_category (category_code, category_name, fulfillment_type, generation_mode, payload_schema, import_config, extract_policy, status)
SELECT 'TEXT_CARD_SECRET',
       '文本卡密',
       'TEXT_SECRET',
       'TEXT_IMPORTED',
       '{"label":{"type":"string","default":"卡密"}}',
       '{"delimiter":"\\n","deduplicateInBatch":true,"deduplicateGlobal":true,"caseSensitive":true,"maxItemLength":512}',
       '{"allowExtract":true,"consumeOnFirstExtract":true,"allowCopyUrl":true}',
       'active'
WHERE NOT EXISTS (SELECT 1 FROM credential_category WHERE category_code = 'TEXT_CARD_SECRET');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2030', 5, '凭证批次', 'credential_batch', '/system/credentials/batches', 'GiftOutlined', 80, 'menu', 'credential_batch', 'credential:batch:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证批次管理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_batch');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2031', 5, '凭证明细', 'credential_item', '/system/credentials/items', 'KeyOutlined', 81, 'menu', 'credential_item', 'credential:item:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证明细管理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_item');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2032', 5, '提取链接', 'credential_extract_link', '/system/credentials/extract-links', 'LinkOutlined', 82, 'menu', 'credential_extract_link', 'credential:extract-link:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证提取链接管理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_extract_link');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2033', 5, '凭证分类', 'credential_category', '/system/credentials/categories', 'AppstoreOutlined', 83, 'menu', 'credential_category', 'credential:category:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证分类配置入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_category');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2034', 5, '导入任务', 'credential_import_task', '/system/credentials/import-tasks', 'ImportOutlined', 84, 'menu', 'credential_import_task', 'credential:import-task:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证导入任务查询入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_import_task');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2035', 5, '兑换记录', 'credential_redeem_record', '/system/credentials/redeem-records', 'FileSearchOutlined', 85, 'menu', 'credential_redeem_record', 'credential:redeem-record:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '凭证兑换记录查询入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'credential_redeem_record');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2036', 5, '风控告警', 'risk_alert_admin', '/system/risk-alerts', 'WarningOutlined', 110, 'menu', 'risk_alert_admin', 'risk:alert:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '异常兑换和高价值操作风控告警入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'risk_alert_admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu
WHERE code IN (
    'credential_batch',
    'credential_item',
    'credential_extract_link',
    'credential_category',
    'credential_import_task',
    'credential_redeem_record',
    'risk_alert_admin'
);

INSERT IGNORE INTO sys_role_route (role_id, route_code) VALUES
(1, 'credential_batch'),
(1, 'credential_item'),
(1, 'credential_extract_link'),
(1, 'credential_category'),
(1, 'credential_import_task'),
(1, 'credential_redeem_record'),
(1, 'risk_alert_admin');

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'credential:category:view', '查看凭证分类'),
(1, 'credential:category:manage', '管理凭证分类'),
(1, 'credential:batch:view', '查看凭证批次'),
(1, 'credential:batch:create', '创建凭证批次'),
(1, 'credential:batch:import', '导入卡密批次'),
(1, 'credential:batch:disable', '停用凭证批次'),
(1, 'credential:item:view', '查看凭证明细'),
(1, 'credential:item:reveal', '查看凭证明文'),
(1, 'credential:item:disable', '停用凭证明细'),
(1, 'credential:extract-link:view', '查看提取链接'),
(1, 'credential:extract-link:create', '生成提取链接'),
(1, 'credential:extract-link:copy', '复制提取链接'),
(1, 'credential:extract-link:disable', '停用提取链接'),
(1, 'credential:extract-link:extend', '延期提取链接'),
(1, 'credential:extract-link:reissue', '补发提取链接'),
(1, 'credential:access-record:view', '查看提取访问记录'),
(1, 'credential:import-task:view', '查看导入任务'),
(1, 'credential:redeem-record:view', '查看兑换记录'),
(1, 'risk:alert:view', '查看风控告警');
