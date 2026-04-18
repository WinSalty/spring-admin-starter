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
