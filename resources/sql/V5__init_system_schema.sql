CREATE TABLE IF NOT EXISTS sys_user_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '用户名称',
    code VARCHAR(60) NOT NULL COMMENT '登录账号',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(255) NOT NULL COMMENT '描述',
    department VARCHAR(40) NOT NULL DEFAULT '' COMMENT '所属部门',
    role_names VARCHAR(60) NOT NULL DEFAULT '' COMMENT '角色名称',
    last_login_at DATETIME NULL COMMENT '最近登录时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_record_record_code (record_code),
    UNIQUE KEY uk_sys_user_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户记录表';

CREATE TABLE IF NOT EXISTS sys_role_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '角色名称',
    code VARCHAR(60) NOT NULL COMMENT '角色编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
    data_scope VARCHAR(60) NOT NULL DEFAULT '' COMMENT '数据范围',
    user_count BIGINT NOT NULL DEFAULT 0 COMMENT '关联用户数',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_record_record_code (record_code),
    UNIQUE KEY uk_sys_role_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色记录表';

CREATE TABLE IF NOT EXISTS sys_dict_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '字典名称',
    code VARCHAR(60) NOT NULL COMMENT '字典编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
    dict_type VARCHAR(60) NOT NULL DEFAULT '' COMMENT '字典类型',
    item_count BIGINT NOT NULL DEFAULT 0 COMMENT '字典项数量',
    cache_key VARCHAR(80) NOT NULL DEFAULT '' COMMENT '缓存键',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_dict_record_record_code (record_code),
    UNIQUE KEY uk_sys_dict_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统字典记录表';

CREATE TABLE IF NOT EXISTS sys_log_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '日志名称',
    code VARCHAR(60) NOT NULL COMMENT '日志编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '操作人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
    log_type VARCHAR(16) NOT NULL COMMENT '日志类型',
    target VARCHAR(180) NOT NULL DEFAULT '' COMMENT '目标对象',
    ip_address VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'IP 地址',
    device_info VARCHAR(255) NOT NULL DEFAULT '' COMMENT '设备信息',
    request_info TEXT COMMENT '请求信息',
    response_info TEXT COMMENT '响应信息',
    result VARCHAR(20) NOT NULL DEFAULT '' COMMENT '结果',
    duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '耗时',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_log_record_record_code (record_code),
    KEY idx_sys_log_record_query (deleted, log_type, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志记录表';
