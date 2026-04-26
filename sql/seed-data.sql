-- resources/sql/V2__seed_auth_permission_data.sql
DELETE FROM sys_role_route;
DELETE FROM sys_role_action;
DELETE FROM sys_role_menu;
DELETE FROM sys_user_role;
DELETE FROM sys_notice;
DELETE FROM sys_department;
DELETE FROM sys_menu;
DELETE FROM sys_role;
DELETE FROM sys_user;

INSERT INTO sys_department (id, name, code, parent_id, sort_order, leader, phone, email, status, created_at, updated_at) VALUES
(1, '平台技术部', 'platform_tech', NULL, 1, '孙胜贤', '13800000001', 'platform@example.com', 'active', '2026-04-18 09:00:00', '2026-04-18 09:00:00'),
(2, '运营中心', 'operation_center', NULL, 2, '运营负责人', '13800000002', 'operation@example.com', 'active', '2026-04-18 09:00:00', '2026-04-18 09:00:00'),
(3, '安全中心', 'security_center', 1, 10, '安全负责人', '13800000003', 'security@example.com', 'active', '2026-04-18 09:00:00', '2026-04-18 09:00:00');

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
('15', 'M1015', 5, '部门管理', 'system_department', '/system/departments', 'ApartmentOutlined', 80, 'menu', 'departments', 'system:department:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '部门管理入口'),
('16', 'M1016', 5, '公告通知', 'system_notice', '/system/notices', 'NotificationOutlined', 90, 'menu', 'notices', 'system:notice:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '公告通知入口'),
('17', 'M1017', NULL, '个人中心', 'account', NULL, 'UserOutlined', 4, 'catalog', NULL, NULL, 0, '/account/settings', 1, NULL, NULL, 0, 'active', '平台技术部', '个人中心目录'),
('18', 'M1018', 17, '个人设置', 'account_settings', '/account/settings', 'UserOutlined', 10, 'menu', 'account_settings', 'account_settings:view', 0, NULL, 1, NULL, NULL, 0, 'active', '平台技术部', '个人资料和通知设置入口');


INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18),
(2, 1), (2, 17), (2, 18);

INSERT INTO sys_role_route (role_id, route_code) VALUES
(1, 'dashboard'), (1, 'query'), (1, 'statistics'), (1, 'permission'), (1, 'users'), (1, 'roles'), (1, 'menus'), (1, 'dicts'), (1, 'logs'), (1, 'configs'), (1, 'detail'), (1, 'departments'), (1, 'notices'), (1, 'account_settings'),
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
(1, 'system:department:add', '新增部门'),
(1, 'system:department:edit', '编辑部门'),
(1, 'system:department:status', '切换部门状态'),
(1, 'system:notice:add', '新增公告'),
(1, 'system:notice:edit', '编辑公告'),
(1, 'system:notice:status', '切换公告状态'),
(1, 'account_settings:view', '查看个人设置'),
(2, 'account_settings:view', '查看个人设置');

INSERT INTO sys_notice (id, title, content, notice_type, priority, is_required, publisher_id, publish_time, expire_time, status, sort_order, created_at, updated_at) VALUES
(1, '系统初始化完成', '后台基础权限、字典、配置和文件模块已经完成初始化。', 'system', 'normal', 0, 1, '2026-04-18 09:30:00', NULL, 'active', 1, '2026-04-18 09:30:00', '2026-04-18 09:30:00'),
(2, '权限联调提醒', '调整角色权限后请重新拉取 permission bootstrap 验证菜单和按钮权限。', 'security', 'high', 1, 1, '2026-04-18 09:40:00', NULL, 'active', 2, '2026-04-18 09:40:00', '2026-04-18 09:40:00');

-- resources/sql/V4__seed_query_data.sql
DELETE FROM biz_query_record;

INSERT INTO biz_query_record (record_code, name, code, status, owner, description, call_count, created_at, updated_at) VALUES
('Q1001', '客户基础信息查询', 'customer_profile_query', 'active', '运营中心', '按客户编号、手机号和证件号检索客户基础资料。', 1288, '2026-03-18 09:12:00', '2026-04-15 16:40:00'),
('Q1002', '订单流水查询', 'order_flow_query', 'active', '交易中心', '查询订单创建、支付、退款和关闭等完整流水。', 2460, '2026-03-20 10:08:00', '2026-04-16 10:10:00'),
('Q1003', '权限变更审计', 'permission_audit_query', 'active', '安全中心', '检索角色、菜单、按钮权限的变更记录。', 638, '2026-03-25 14:32:00', '2026-04-14 11:02:00'),
('Q1004', '登录异常查询', 'login_risk_query', 'active', '安全中心', '查询异地登录、连续失败和风险设备登录记录。', 932, '2026-03-29 08:24:00', '2026-04-16 08:45:00'),
('Q1005', '库存快照查询', 'stock_snapshot_query', 'disabled', '供应链', '历史库存快照查询，已由新库存中心接口替代。', 84, '2026-02-17 15:20:00', '2026-04-01 09:18:00'),
('Q1006', '商品上下架查询', 'goods_publish_query', 'active', '商品中心', '查看商品上下架状态、操作人和最近一次发布时间。', 1186, '2026-03-11 13:00:00', '2026-04-12 18:22:00'),
('Q1007', '营销活动查询', 'campaign_query', 'active', '营销中心', '查询活动状态、预算消耗和人群覆盖情况。', 760, '2026-03-15 11:36:00', '2026-04-13 17:31:00'),
('Q1008', '消息投递查询', 'message_delivery_query', 'disabled', '消息中心', '查询短信、站内信、邮件等消息通道投递记录。', 352, '2026-02-28 16:18:00', '2026-04-07 12:09:00'),
('Q1009', '支付对账查询', 'payment_reconcile_query', 'active', '财务中心', '按支付渠道、交易日期和对账状态查询资金记录。', 1094, '2026-03-05 09:56:00', '2026-04-15 19:42:00'),
('Q1010', '接口调用日志', 'api_access_log_query', 'active', '平台技术部', '按接口、调用方、响应码和耗时检索访问日志。', 3198, '2026-03-01 10:00:00', '2026-04-16 09:06:00'),
('Q1011', '审批流程查询', 'approval_flow_query', 'active', '流程中心', '查询审批节点、当前处理人和流程耗时。', 476, '2026-03-23 17:48:00', '2026-04-11 13:44:00'),
('Q1012', '数据导出任务', 'export_task_query', 'active', '数据平台', '查看异步导出任务进度、文件状态和失败原因。', 884, '2026-03-27 15:22:00', '2026-04-16 11:27:00');

-- resources/sql/V6__seed_system_data.sql
DELETE FROM sys_user_record;
DELETE FROM sys_role_record;
DELETE FROM sys_dict_record;
DELETE FROM sys_log_record;

INSERT INTO sys_user_record (record_code, name, code, status, owner, description, department, role_names, last_login_at, created_at, updated_at) VALUES
('U1001', '孙管理员', 'admin', 'active', '平台技术部', '系统默认管理员账号，拥有全部后台管理权限。', '平台技术部', '超级管理员', '2026-04-16 20:18:00', '2026-03-01 09:00:00', '2026-04-16 20:18:00'),
('U1002', '运营观察员', 'viewer', 'active', '运营中心', '只读访客账号，用于验证菜单、路由和按钮权限收敛效果。', '运营中心', '只读访客', '2026-04-16 18:42:00', '2026-03-06 10:12:00', '2026-04-16 18:42:00'),
('U1003', '安全审计员', 'audit_user', 'disabled', '安全中心', '负责查看权限变更和登录风险审计记录。', '安全中心', '审计员', '2026-04-10 11:30:00', '2026-03-12 15:32:00', '2026-04-12 09:20:00');

INSERT INTO sys_role_record (record_code, name, code, status, owner, description, data_scope, user_count, created_at, updated_at) VALUES
('R1001', '超级管理员', 'super_admin', 'active', '平台技术部', '拥有全部菜单、路由和按钮权限。', '全部数据', 1, '2026-03-01 09:10:00', '2026-04-16 17:36:00'),
('R1002', '只读访客', 'readonly_viewer', 'active', '运营中心', '仅允许查看工作台，不能进入管理页面。', '本人数据', 1, '2026-03-05 13:22:00', '2026-04-15 10:14:00'),
('R1003', '系统审计员', 'system_auditor', 'disabled', '安全中心', '用于登录日志、操作日志和接口日志审计。', '部门数据', 0, '2026-03-18 16:02:00', '2026-04-08 11:44:00');

INSERT INTO sys_dict_record (record_code, name, code, status, owner, description, dict_type, item_count, cache_key, created_at, updated_at) VALUES
('D1001', '用户状态', 'user_status', 'active', '平台技术部', '用户启用、停用等状态字典。', '系统字典', 2, 'dict:user_status', '2026-03-02 10:10:00', '2026-04-12 09:00:00'),
('D1002', '日志类型', 'log_type', 'active', '安全中心', '登录日志、操作日志和接口日志分类。', '业务字典', 3, 'dict:log_type', '2026-03-04 11:28:00', '2026-04-14 16:48:00'),
('D1003', '审批状态', 'approval_status', 'disabled', '流程中心', '待提交、审批中、已通过、已驳回等审批状态。', '业务字典', 4, 'dict:approval_status', '2026-03-08 14:16:00', '2026-04-02 13:22:00');

INSERT INTO sys_log_record (record_code, name, code, status, owner, description, log_type, target, ip_address, device_info, request_info, response_info, result, duration_ms, created_at, updated_at) VALUES
('L1001', 'admin 登录成功', 'login_admin_success', 'active', 'admin', '管理员从可信设备登录系统。', 'login', '认证中心', '192.168.1.10', 'Chrome on macOS', '{"method":"POST","uri":"/api/auth/login"}', '{"message":"登录成功"}', '成功', 86, '2026-04-16 20:18:00', '2026-04-16 20:18:00'),
('L1002', '编辑查询配置', 'query_config_update', 'active', 'admin', '更新客户基础信息查询配置说明。', 'operation', '查询管理', '192.168.1.10', 'Chrome on macOS', '{"method":"POST","uri":"/api/query/save"}', '{"message":"保存成功"}', '成功', 132, '2026-04-16 19:44:00', '2026-04-16 19:44:00'),
('L1003', '权限接口访问', 'permission_bootstrap_api', 'disabled', 'viewer', '访客账号请求权限 bootstrap 接口。', 'api', '/api/permission/bootstrap', '192.168.1.22', 'Safari on iPhone', '{"method":"GET","uri":"/api/permission/bootstrap"}', '{"message":"无权限访问该资源"}', '拒绝', 48, '2026-04-16 18:42:00', '2026-04-16 18:42:00');

-- resources/sql/V8__seed_config_data.sql
DELETE FROM sys_config_record;

INSERT INTO sys_config_record (record_code, name, code, config_type, value_type, config_value, description, created_at, updated_at) VALUES
('C1001', '系统名称', 'app.name', 'basic', 'string', 'React Admin Starter', '展示在后台布局和浏览器标题中的系统名称。', '2026-04-17 10:20:00', '2026-04-17 10:20:00'),
('C1002', '默认首页路径', 'app.homePath', 'basic', 'string', '/dashboard', '登录成功后默认跳转的首页路径。', '2026-04-17 10:20:00', '2026-04-17 10:20:00'),
('C1003', '严格权限校验', 'security.strictPermission', 'switch', 'boolean', 'true', '开启后对菜单、路由和按钮权限执行更严格的校验。', '2026-04-17 10:22:00', '2026-04-17 10:22:00'),
('C1004', '登录日志开关', 'audit.loginLog', 'switch', 'boolean', 'true', '控制登录日志是否写入审计链路。', '2026-04-17 10:22:00', '2026-04-17 10:22:00'),
('C1005', '菜单缓存版本', 'cache.menuVersion', 'cache', 'number', '3', '前端菜单缓存版本号，变更后可触发客户端刷新菜单缓存。', '2026-04-17 10:25:00', '2026-04-17 10:25:00'),
('C1006', '字典缓存 TTL(秒)', 'cache.dictTtlSeconds', 'cache', 'number', '600', '控制字典缓存有效期，便于联调缓存刷新策略。', '2026-04-17 10:25:00', '2026-04-17 10:25:00'),
('C1007', '刷新令牌有效期(秒)', 'security.refreshTokenExpireSeconds', 'cache', 'number', '604800', '控制 refresh token 默认有效期，便于本地联调会话失效策略。', '2026-04-17 10:26:00', '2026-04-17 10:26:00');

-- extended dict/param/file seed data

DELETE FROM sys_dict_data;
DELETE FROM sys_dict_type;
DELETE FROM sys_config;
DELETE FROM sys_file;

INSERT INTO sys_dict_type (id, dict_code, dict_name, dict_type, status, remark, created_at, updated_at) VALUES
(1, 'DT1001', '系统状态', 'sys_status', 'active', '通用启停状态。', '2026-04-18 09:00:00', '2026-04-18 09:00:00'),
(2, 'DT1002', '日志类型', 'log_type', 'active', '登录、操作、接口日志分类。', '2026-04-18 09:00:00', '2026-04-18 09:00:00'),
(3, 'DT1003', '文件状态', 'file_status', 'active', '文件启用和停用状态。', '2026-04-18 09:00:00', '2026-04-18 09:00:00'),
(4, 'DT1004', '参数类型', 'config_type', 'active', '参数配置分类。', '2026-04-18 09:00:00', '2026-04-18 09:00:00');

INSERT INTO sys_dict_data (data_code, dict_type_id, dict_type, label, value, sort_no, status, remark, created_at, updated_at) VALUES
('DD1001', 1, 'sys_status', '启用', 'active', 1, 'active', '可用状态。', '2026-04-18 09:05:00', '2026-04-18 09:05:00'),
('DD1002', 1, 'sys_status', '停用', 'disabled', 2, 'active', '不可用状态。', '2026-04-18 09:05:00', '2026-04-18 09:05:00'),
('DD1003', 2, 'log_type', '登录日志', 'login', 1, 'active', '用户登录登出日志。', '2026-04-18 09:06:00', '2026-04-18 09:06:00'),
('DD1004', 2, 'log_type', '操作日志', 'operation', 2, 'active', '业务操作日志。', '2026-04-18 09:06:00', '2026-04-18 09:06:00'),
('DD1005', 2, 'log_type', '接口日志', 'api', 3, 'active', '接口访问日志。', '2026-04-18 09:06:00', '2026-04-18 09:06:00'),
('DD1006', 3, 'file_status', '启用', 'active', 1, 'active', '文件可下载。', '2026-04-18 09:07:00', '2026-04-18 09:07:00'),
('DD1007', 3, 'file_status', '停用', 'disabled', 2, 'active', '文件暂不可用。', '2026-04-18 09:07:00', '2026-04-18 09:07:00'),
('DD1008', 4, 'config_type', '基础配置', 'basic', 1, 'active', '基础展示配置。', '2026-04-18 09:08:00', '2026-04-18 09:08:00'),
('DD1009', 4, 'config_type', '安全配置', 'security', 2, 'active', '认证和权限配置。', '2026-04-18 09:08:00', '2026-04-18 09:08:00'),
('DD1010', 4, 'config_type', '缓存配置', 'cache', 3, 'active', '缓存参数配置。', '2026-04-18 09:08:00', '2026-04-18 09:08:00');

INSERT INTO sys_config (id, config_code, config_name, config_key, config_value, value_type, config_type, status, remark, created_at, updated_at) VALUES
(1, 'P1001', '系统名称', 'app.name', 'Spring Admin Starter', 'string', 'basic', 'active', '后台系统显示名称。', '2026-04-18 09:10:00', '2026-04-18 09:10:00'),
(2, 'P1002', '默认首页', 'app.homePath', '/dashboard', 'string', 'basic', 'active', '登录成功后的默认页面。', '2026-04-18 09:10:00', '2026-04-18 09:10:00'),
(3, 'P1003', '注册开关', 'security.registerEnabled', 'true', 'boolean', 'security', 'active', '控制注册接口是否开放。', '2026-04-18 09:11:00', '2026-04-18 09:11:00'),
(4, 'P1004', '字典缓存 TTL', 'cache.dictTtlSeconds', '3600', 'number', 'cache', 'active', '字典缓存秒级有效期。', '2026-04-18 09:12:00', '2026-04-18 09:12:00'),
(5, 'P1005', '上传大小限制', 'file.maxSizeMb', '10', 'number', 'basic', 'active', '单文件上传大小限制，单位 MB。', '2026-04-18 09:13:00', '2026-04-18 09:13:00');
