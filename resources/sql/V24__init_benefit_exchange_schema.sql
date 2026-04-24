CREATE TABLE IF NOT EXISTS benefit_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    product_no VARCHAR(64) NOT NULL COMMENT '商品编号',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    benefit_type VARCHAR(32) NOT NULL COMMENT '权益类型',
    benefit_code VARCHAR(128) NOT NULL COMMENT '权益编码',
    benefit_name VARCHAR(128) NOT NULL COMMENT '权益名称',
    benefit_config JSON NULL COMMENT '权益配置',
    cost_points BIGINT NOT NULL COMMENT '消耗积分',
    stock_total INT NOT NULL DEFAULT -1 COMMENT '总库存，-1表示不限',
    stock_used INT NOT NULL DEFAULT 0 COMMENT '已用库存',
    valid_from DATETIME NOT NULL COMMENT '生效时间',
    valid_to DATETIME NOT NULL COMMENT '失效时间',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_benefit_product_no (product_no),
    KEY idx_benefit_product_status_time (status, valid_from, valid_to),
    KEY idx_benefit_product_type (benefit_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益兑换商品表';

CREATE TABLE IF NOT EXISTS benefit_exchange_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no VARCHAR(64) NOT NULL COMMENT '兑换单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '权益商品ID',
    product_no VARCHAR(64) NOT NULL COMMENT '权益商品编号',
    benefit_type VARCHAR(32) NOT NULL COMMENT '权益类型',
    benefit_code VARCHAR(128) NOT NULL COMMENT '权益编码',
    cost_points BIGINT NOT NULL COMMENT '消耗积分',
    freeze_no VARCHAR(64) NOT NULL COMMENT '冻结单号',
    status VARCHAR(32) NOT NULL COMMENT '状态',
    failure_message VARCHAR(256) NOT NULL DEFAULT '' COMMENT '失败原因',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '幂等键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_benefit_exchange_order_no (order_no),
    UNIQUE KEY uk_benefit_exchange_user_idempotency (user_id, idempotency_key),
    KEY idx_benefit_exchange_user_created_at (user_id, created_at),
    KEY idx_benefit_exchange_status_created_at (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权益兑换订单表';

CREATE TABLE IF NOT EXISTS user_benefit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    benefit_type VARCHAR(32) NOT NULL COMMENT '权益类型',
    benefit_code VARCHAR(128) NOT NULL COMMENT '权益编码',
    benefit_name VARCHAR(128) NOT NULL COMMENT '权益名称',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型',
    source_no VARCHAR(64) NOT NULL COMMENT '来源单号',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态',
    effective_at DATETIME NOT NULL COMMENT '生效时间',
    expire_at DATETIME DEFAULT NULL COMMENT '失效时间',
    config_snapshot JSON NULL COMMENT '配置快照',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_benefit_source (source_type, source_no, benefit_code),
    KEY idx_user_benefit_user_status (user_id, status, benefit_type),
    KEY idx_user_benefit_expire (status, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权益表';

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2005', (SELECT id FROM sys_menu WHERE code = 'account' LIMIT 1), '权益兑换', 'benefit_center', '/points/benefits', 'ShopOutlined', 21, 'menu', 'benefit_center', 'benefit:center:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '用户积分权益兑换入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'benefit_center');

INSERT INTO sys_menu (record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description)
SELECT 'M2006', 5, '权益商品', 'benefit_product_admin', '/system/benefits/products', 'ShopOutlined', 105, 'menu', 'benefit_product_admin', 'benefit:product:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '权益兑换商品管理入口'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code = 'benefit_product_admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE code IN ('benefit_center', 'benefit_product_admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu WHERE code = 'benefit_center';

INSERT IGNORE INTO sys_role_route (role_id, route_code) VALUES
(1, 'benefit_center'), (1, 'benefit_product_admin'),
(2, 'benefit_center');

INSERT IGNORE INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'benefit:product:create', '创建权益商品'),
(1, 'benefit:product:update', '更新权益商品'),
(1, 'benefit:product:status', '变更权益商品状态'),
(1, 'benefit:order:view', '查看权益兑换订单');
