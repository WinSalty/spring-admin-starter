USE `spring_admin`;

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `remark`, `status`, `deleted`, `create_by`)
VALUES
  (1, '超级管理员', 'SUPER_ADMIN', '系统内置超级管理员角色', 1, 0, 1),
  (2, '管理员', 'ADMIN', '系统管理员角色', 1, 0, 1),
  (3, '普通用户', 'USER', '默认普通用户角色', 1, 0, 1)
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `sys_user` (`id`, `username`, `password`, `email`, `nick_name`, `status`, `deleted`, `create_by`)
VALUES
  (1, 'admin', '$2a$10$uhEZK0varIylnsr4oIIH.eNxPvHEjcYUebIqul6AI0mYmamsGDhO2', 'admin@example.com', '超级管理员', 1, 0, 1)
ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `nick_name` = VALUES(`nick_name`),
  `status` = VALUES(`status`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_by`)
VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE `create_by` = VALUES(`create_by`);

INSERT INTO `sys_menu` (`id`, `menu_name`, `menu_code`, `type`, `path`, `component`, `parent_id`, `icon`, `sort`, `permission_code`, `status`, `deleted`, `create_by`)
VALUES
  (1, '工作台', 'dashboard', 'menu', '/dashboard', 'dashboard/index', 0, 'DashboardOutlined', 10, 'dashboard', 1, 0, 1),
  (2, '查询管理', 'query', 'menu', '/query', 'query/index', 0, 'SearchOutlined', 20, 'query', 1, 0, 1),
  (3, '数据统计', 'statistics', 'menu', '/statistics', 'statistics/index', 0, 'BarChartOutlined', 30, 'statistics', 1, 0, 1),
  (4, '权限目录', 'permission', 'catalog', '/permission', 'Layout', 0, 'SafetyCertificateOutlined', 40, 'permission', 1, 0, 1),
  (5, '用户管理', 'users', 'menu', '/permission/users', 'system/users/index', 4, 'UserOutlined', 10, 'users', 1, 0, 1),
  (6, '角色管理', 'roles', 'menu', '/permission/roles', 'system/roles/index', 4, 'TeamOutlined', 20, 'roles', 1, 0, 1),
  (7, '菜单管理', 'menus', 'menu', '/permission/menus', 'system/menus/index', 4, 'MenuOutlined', 30, 'menus', 1, 0, 1),
  (8, '字典管理', 'dicts', 'menu', '/permission/dicts', 'system/dicts/index', 4, 'ProfileOutlined', 40, 'dicts', 1, 0, 1),
  (9, '日志管理', 'logs', 'menu', '/permission/logs', 'system/logs/index', 4, 'FileSearchOutlined', 50, 'logs', 1, 0, 1),
  (10, '系统配置', 'configs', 'menu', '/permission/configs', 'system/configs/index', 4, 'SettingOutlined', 60, 'configs', 1, 0, 1),
  (101, '用户新增', 'system:user:add', 'button', NULL, NULL, 5, NULL, 10, 'system:user:add', 1, 0, 1),
  (102, '用户编辑', 'system:user:edit', 'button', NULL, NULL, 5, NULL, 20, 'system:user:edit', 1, 0, 1),
  (103, '用户删除', 'system:user:delete', 'button', NULL, NULL, 5, NULL, 30, 'system:user:delete', 1, 0, 1),
  (201, '角色授权', 'system:role:permission', 'button', NULL, NULL, 6, NULL, 10, 'system:role:permission', 1, 0, 1)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`),
  `type` = VALUES(`type`),
  `path` = VALUES(`path`),
  `component` = VALUES(`component`),
  `parent_id` = VALUES(`parent_id`),
  `icon` = VALUES(`icon`),
  `sort` = VALUES(`sort`),
  `permission_code` = VALUES(`permission_code`),
  `status` = VALUES(`status`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_by`)
SELECT 1, `id`, 1 FROM `sys_menu`
ON DUPLICATE KEY UPDATE `create_by` = VALUES(`create_by`);

INSERT INTO `sys_dept` (`id`, `dept_name`, `parent_id`, `ancestors`, `leader`, `sort`, `status`, `deleted`, `create_by`)
VALUES (1, '总部', 0, '0', 'admin', 10, 1, 0, 1)
ON DUPLICATE KEY UPDATE
  `dept_name` = VALUES(`dept_name`),
  `leader` = VALUES(`leader`),
  `status` = VALUES(`status`),
  `deleted` = VALUES(`deleted`);

INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `deleted`, `create_by`)
VALUES (1, '通用状态', 'sys_common_status', 1, 'active/disabled 通用状态', 0, 1)
ON DUPLICATE KEY UPDATE `dict_name` = VALUES(`dict_name`), `status` = VALUES(`status`);

INSERT INTO `sys_dict_item` (`dict_type_id`, `dict_label`, `dict_value`, `list_class`, `sort`, `status`, `remark`, `deleted`, `create_by`)
VALUES
  (1, '启用', 'active', 'success', 10, 1, '启用状态', 0, 1),
  (1, '禁用', 'disabled', 'danger', 20, 1, '禁用状态', 0, 1)
ON DUPLICATE KEY UPDATE
  `dict_label` = VALUES(`dict_label`),
  `list_class` = VALUES(`list_class`),
  `sort` = VALUES(`sort`),
  `status` = VALUES(`status`);

INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `remark`, `deleted`, `create_by`)
VALUES
  ('系统名称', 'system.name', 'Spring Admin Starter', 'system', '后台系统名称', 0, 1),
  ('默认密码策略', 'security.password.policy', 'bcrypt', 'system', '密码加密策略', 0, 1)
ON DUPLICATE KEY UPDATE
  `config_name` = VALUES(`config_name`),
  `config_value` = VALUES(`config_value`),
  `remark` = VALUES(`remark`);
