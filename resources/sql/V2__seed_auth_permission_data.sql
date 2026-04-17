DELETE FROM sys_role_action;
DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_menu;
DELETE FROM sys_role;
DELETE FROM sys_user;

INSERT INTO sys_role (id, role_code, role_name, status) VALUES
(1, 'admin', '管理员', 'active'),
(2, 'viewer', '只读用户', 'active');

INSERT INTO sys_user (id, username, email, password, nickname, status) VALUES
(1, 'admin', 'admin@example.com', '$2y$10$qFB1d3QTQTB0Q5lD.p9TdOl4wGzUtl43VGzyLderpTfnwMOFdFKRW', '系统管理员', 'active'),
(2, 'viewer', 'viewer@example.com', '$2y$10$qFB1d3QTQTB0Q5lD.p9TdOl4wGzUtl43VGzyLderpTfnwMOFdFKRW', '只读用户', 'active');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 2);

INSERT INTO sys_menu (id, parent_id, title, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status) VALUES
(1, NULL, '工作台', '/dashboard', 'DashboardOutlined', 1, 'menu', 'dashboard', 'dashboard:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(2, NULL, '查询管理', '/query', 'SearchOutlined', 2, 'menu', 'query', 'query:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(3, NULL, '权限管理', '/permission', 'SafetyCertificateOutlined', 3, 'menu', 'permission', 'permission:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(4, NULL, '系统管理', '/system', 'SettingOutlined', 4, 'catalog', NULL, 'system:view', 0, '/system/users', 1, NULL, NULL, 0, 'active'),
(5, 4, '用户管理', '/system/users', 'UserOutlined', 1, 'menu', 'users', 'users:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(6, 4, '角色管理', '/system/roles', 'TeamOutlined', 2, 'menu', 'roles', 'roles:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(7, 4, '菜单管理', '/system/menus', 'MenuOutlined', 3, 'menu', 'menus', 'menus:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(8, 4, '字典管理', '/system/dicts', 'BookOutlined', 4, 'menu', 'dicts', 'dicts:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(9, 4, '日志管理', '/system/logs', 'FileTextOutlined', 5, 'menu', 'logs', 'logs:view', 0, NULL, 1, NULL, NULL, 0, 'active'),
(10, 4, '系统配置', '/system/configs', 'ControlOutlined', 6, 'menu', 'configs', 'configs:view', 0, NULL, 1, NULL, NULL, 0, 'active');

INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(2, 1), (2, 2);

INSERT INTO sys_role_action (role_id, action_code, action_name) VALUES
(1, 'query:add', '新增查询'),
(1, 'query:edit', '编辑查询'),
(1, 'permission:save', '保存权限'),
(1, 'users:save', '保存用户'),
(1, 'roles:save', '保存角色'),
(1, 'menus:save', '保存菜单'),
(1, 'configs:save', '保存配置'),
(2, 'dashboard:view', '查看工作台'),
(2, 'query:view', '查看查询');
