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

INSERT INTO sys_log_record (record_code, name, code, status, owner, description, log_type, target, ip_address, result, duration_ms, created_at, updated_at) VALUES
('L1001', 'admin 登录成功', 'login_admin_success', 'active', 'admin', '管理员从可信设备登录系统。', 'login', '认证中心', '192.168.1.10', '成功', 86, '2026-04-16 20:18:00', '2026-04-16 20:18:00'),
('L1002', '编辑查询配置', 'query_config_update', 'active', 'admin', '更新客户基础信息查询配置说明。', 'operation', '查询管理', '192.168.1.10', '成功', 132, '2026-04-16 19:44:00', '2026-04-16 19:44:00'),
('L1003', '权限接口访问', 'permission_bootstrap_api', 'disabled', 'viewer', '访客账号请求权限 bootstrap 接口。', 'api', '/api/permission/bootstrap', '192.168.1.22', '拒绝', 48, '2026-04-16 18:42:00', '2026-04-16 18:42:00');
