DELETE FROM sys_config_record;

INSERT INTO sys_config_record (record_code, name, code, config_type, value_type, config_value, description, created_at, updated_at) VALUES
('C1001', '系统名称', 'app.name', 'basic', 'string', 'React Admin Starter', '展示在后台布局和浏览器标题中的系统名称。', '2026-04-17 10:20:00', '2026-04-17 10:20:00'),
('C1002', '默认首页路径', 'app.homePath', 'basic', 'string', '/dashboard', '登录成功后默认跳转的首页路径。', '2026-04-17 10:20:00', '2026-04-17 10:20:00'),
('C1003', '严格权限校验', 'security.strictPermission', 'switch', 'boolean', 'true', '开启后对菜单、路由和按钮权限执行更严格的校验。', '2026-04-17 10:22:00', '2026-04-17 10:22:00'),
('C1004', '登录日志开关', 'audit.loginLog', 'switch', 'boolean', 'true', '控制登录日志是否写入审计链路。', '2026-04-17 10:22:00', '2026-04-17 10:22:00'),
('C1005', '菜单缓存版本', 'cache.menuVersion', 'cache', 'number', '3', '前端菜单缓存版本号，变更后可触发客户端刷新菜单缓存。', '2026-04-17 10:25:00', '2026-04-17 10:25:00'),
('C1006', '字典缓存 TTL(秒)', 'cache.dictTtlSeconds', 'cache', 'number', '600', '控制字典缓存有效期，便于联调缓存刷新策略。', '2026-04-17 10:25:00', '2026-04-17 10:25:00');
