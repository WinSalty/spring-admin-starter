USE `spring_admin`;

CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  `username` varchar(64) DEFAULT NULL COMMENT '用户名',
  `login_ip` varchar(64) DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(128) DEFAULT NULL COMMENT '登录地点',
  `browser` varchar(64) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(64) DEFAULT NULL COMMENT '操作系统',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1成功 0失败',
  `msg` varchar(255) DEFAULT NULL COMMENT '消息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_login_log_username` (`username`),
  KEY `idx_sys_login_log_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

CREATE TABLE IF NOT EXISTS `sys_oper_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '操作日志ID',
  `title` varchar(128) DEFAULT NULL COMMENT '模块标题',
  `business_type` varchar(32) DEFAULT NULL COMMENT '业务类型',
  `method` varchar(255) DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方式',
  `operator_type` varchar(32) DEFAULT NULL COMMENT '操作类别',
  `oper_name` varchar(64) DEFAULT NULL COMMENT '操作人员',
  `dept_name` varchar(64) DEFAULT NULL COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
  `oper_ip` varchar(64) DEFAULT NULL COMMENT '操作IP',
  `oper_location` varchar(128) DEFAULT NULL COMMENT '操作地点',
  `oper_param` text COMMENT '请求参数',
  `json_result` text COMMENT '响应结果',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态：1正常 0异常',
  `error_msg` text COMMENT '错误消息',
  `oper_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `cost_time` bigint(20) DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_oper_log_oper_name` (`oper_name`),
  KEY `idx_sys_oper_log_oper_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS `sys_log_error` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '异常日志ID',
  `error_module` varchar(128) DEFAULT NULL COMMENT '异常模块',
  `error_method` varchar(255) DEFAULT NULL COMMENT '异常方法',
  `error_url` varchar(255) DEFAULT NULL COMMENT '请求URL',
  `oper_name` varchar(64) DEFAULT NULL COMMENT '操作人员',
  `oper_ip` varchar(64) DEFAULT NULL COMMENT '操作IP',
  `error_msg` text COMMENT '错误消息',
  `error_stack` mediumtext COMMENT '异常堆栈',
  `error_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '异常时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_log_error_error_time` (`error_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常日志表';
