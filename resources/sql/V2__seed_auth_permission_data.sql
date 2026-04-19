DELETE FROM sys_role_route;
DELETE FROM sys_role_action;
DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_menu;
DELETE FROM sys_role;
DELETE FROM sys_user;

INSERT INTO sys_role (id, record_code, role_code, role_name, status, owner, description, data_scope) VALUES
(1, 'R1001', 'admin', '管理员', 'active', '平台技术部', '拥有全部菜单、路由和按钮权限。', '全部数据'),
(2, 'R1002', 'viewer', '只读用户', 'active', '运营中心', '仅允许查看工作台，不能进入管理页面。', '本人数据');

INSERT INTO sys_user (id, record_code, username, email, password, nickname, avatar_url, country, province, city, street_address, phone_prefix, phone_number, notify_account, notify_system, notify_todo, status, owner, description, department_id, last_login_at) VALUES
(1, 'U1001', 'admin', 'admin@example.com', '$2y$10$qFB1d3QTQTB0Q5lD.p9TdOl4wGzUtl43VGzyLderpTfnwMOFdFKRW', '系统管理员', '', '中国', '浙江省', '杭州市', '西湖区工专路 77 号', '86', '13800000001', 1, 1, 1, 'active', '平台技术部', '系统默认管理员账号，拥有全部后台管理权限。', 1, '2026-04-16 20:18:00'),
(2, 'U1002', 'viewer', 'viewer@example.com', '$2y$10$qFB1d3QTQTB0Q5lD.p9TdOl4wGzUtl43VGzyLderpTfnwMOFdFKRW', '只读用户', '', '中国', '浙江省', '杭州市', '西湖区工专路 88 号', '86', '13800000002', 1, 1, 0, 'active', '运营中心', '只读访客账号，用于验证菜单、路由和按钮权限收敛效果。', 2, '2026-04-16 18:42:00');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2);

INSERT INTO sys_menu (id, record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description) VALUES
('1', 'M1001', NULL, '工作台', 'dashboard', '/dashboard', 'DashboardOutlined', 1, 'menu', 'dashboard', 'dashboard:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '工作台首页入口'),
('2', 'M1002', NULL, '业务中心', 'business', NULL, 'AppstoreOutlined', 2, 'catalog', NULL, NULL, 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '业务模块目录'),
('3', 'M1003', 2, '查询管理', 'query', '/query', 'SearchOutlined', 10, 'menu', 'query', 'query:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '查询管理入口'),
('4', 'M1004', 2, '数据统计', 'statistics', '/statistics', 'LineChartOutlined', 20, 'menu', 'statistics', 'statistics:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '统计分析入口'),
('5', 'M1005', NULL, '系统管理', 'system', '/system', 'SettingOutlined', 3, 'catalog', NULL, NULL, 0, '/system/users', 1, NULL, NULL, 0, 'active', '平台技术部', '系统模块目录'),
('6', 'M1006', 5, '权限目录', 'permission', '/permission', 'KeyOutlined', 10, 'menu', 'permission', 'permission:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '权限分配入口'),
('7', 'M1007', 5, '用户管理', 'system_user', '/system/users', 'UserOutlined', 20, 'menu', 'users', 'system:user:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '用户管理入口'),
('8', 'M1008', 5, '角色管理', 'system_role', '/system/roles', 'TeamOutlined', 30, 'menu', 'roles', 'system:role:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '角色管理入口'),
('9', 'M1009', 5, '菜单管理', 'system_menu', '/system/menus', 'MenuOutlined', 40, 'menu', 'menus', 'system:menu:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '菜单管理入口'),
('10', 'M1010', 5, '字典管理', 'system_dict', '/system/dicts', 'BookOutlined', 50, 'menu', 'dicts', 'system:dict:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '字典管理入口'),
('11', 'M1011', 5, '日志管理', 'system_log', '/system/logs', 'FileSearchOutlined', 60, 'menu', 'logs', 'system:log:view', 0, NULL, 1, NULL, NULL, 0, 'active', '安全中心', '日志管理入口'),
('12', 'M1012', 5, '系统配置', 'system_config', '/system/configs', 'SettingOutlined', 70, 'menu', 'configs', 'system:config:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '系统配置入口'),
('13', 'M1013', 11, '审计详情隐藏页', 'system_log_detail', '/system/logs/detail', 'FileSearchOutlined', 10, 'hidden', 'detail', 'system:log:detail', 1, NULL, 1, NULL, NULL, 0, 'active', '安全中心', '日志详情隐藏页'),
('14', 'M1014', NULL, 'Ant Design 文档', 'antd_docs', NULL, 'BookOutlined', 90, 'external', NULL, 'docs:antd:view', 0, NULL, 1, 'https://ant.design', NULL, 0, 'active', '平台技术部', '外链菜单示例'),
('15', 'M1015', NULL, '个人中心', 'account', NULL, 'UserOutlined', 4, 'catalog', NULL, NULL, 0, '/account/settings', 1, NULL, NULL, 0, 'active', '平台技术部', '个人中心目录'),
('16', 'M1016', 15, '个人设置', 'account_settings', '/account/settings', 'UserOutlined', 10, 'menu', 'account_settings', 'account_settings:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '个人资料和通知设置入口');


INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16),
(2, 1), (2, 15), (2, 16);

INSERT INTO sys_role_route (role_id, route_code) VALUES
(1, 'dashboard'), (1, 'query'), (1, 'statistics'), (1, 'permission'), (1, 'users'), (1, 'roles'), (1, 'menus'), (1, 'dicts'), (1, 'logs'), (1, 'configs'), (1, 'detail'), (1, 'account_settings'),
(2, 'dashboard'), (2, 'account_settings');

INSERT INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'query:add', '新增查询'),
(1, 'query:edit', '编辑查询'),
(1, 'query:delete', '删除查询'),
(1, 'query:export', '导出查询'),
(1, 'statistics:view', '查看统计'),
(1, 'statistics:export', '导出统计'),
(1, 'permission:view', '查看权限'),
(1, 'permission:assign', '分配权限'),
(1, 'system:user:add', '新增用户'),
(1, 'system:user:edit', '编辑用户'),
(1, 'system:user:status', '切换用户状态'),
(1, 'system:user:reset', '重置密码'),
(1, 'system:user:assign-role', '分配角色'),
(1, 'system:role:add', '新增角色'),
(1, 'system:role:edit', '编辑角色'),
(1, 'system:role:status', '切换角色状态'),
(1, 'system:role:assign-permission', '分配权限'),
(1, 'system:menu:add', '新增菜单'),
(1, 'system:menu:edit', '编辑菜单'),
(1, 'system:menu:status', '切换菜单状态'),
(1, 'system:dict:add', '新增字典'),
(1, 'system:dict:edit', '编辑字典'),
(1, 'system:dict:status', '切换字典状态'),
(1, 'system:dict:refresh', '刷新字典缓存'),
(1, 'system:config:view', '查看系统配置'),
(1, 'system:config:edit', '编辑系统配置'),
(1, 'account_settings:view', '查看个人设置'),
(2, 'account_settings:view', '查看个人设置');
