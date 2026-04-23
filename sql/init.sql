SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS sys_department;
DROP TABLE IF EXISTS sys_file;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_dict_data;
DROP TABLE IF EXISTS sys_dict_type;
DROP TABLE IF EXISTS sys_config_record;
DROP TABLE IF EXISTS sys_log_archive;
DROP TABLE IF EXISTS sys_log_record;
DROP TABLE IF EXISTS sys_dict_record;
DROP TABLE IF EXISTS sys_role_record;
DROP TABLE IF EXISTS sys_user_record;
DROP TABLE IF EXISTS biz_query_record;
DROP TABLE IF EXISTS sys_role_route;
DROP TABLE IF EXISTS sys_role_action;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;
SET FOREIGN_KEY_CHECKS = 1;

-- resources/sql/V1__init_rbac_schema.sql
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址',
    country VARCHAR(64) NOT NULL DEFAULT '中国' COMMENT '国家/地区',
    province VARCHAR(64) DEFAULT NULL COMMENT '省份',
    city VARCHAR(64) DEFAULT NULL COMMENT '城市',
    street_address VARCHAR(255) DEFAULT NULL COMMENT '街道地址',
    phone_prefix VARCHAR(12) DEFAULT NULL COMMENT '电话区号',
    phone_number VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
    notify_account TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号安全通知',
    notify_system TINYINT(1) NOT NULL DEFAULT 1 COMMENT '系统消息通知',
    notify_todo TINYINT(1) NOT NULL DEFAULT 0 COMMENT '待办任务通知',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(64) NOT NULL DEFAULT '平台技术部' COMMENT '负责人',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    department_id BIGINT DEFAULT NULL COMMENT '部门ID',
    last_login_at DATETIME NULL COMMENT '最近登录时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_record_code (record_code),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(64) NOT NULL DEFAULT '平台技术部' COMMENT '负责人',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    data_scope VARCHAR(60) NOT NULL DEFAULT '全部数据' COMMENT '数据范围',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_record_code (record_code),
    UNIQUE KEY uk_sys_role_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) DEFAULT NULL COMMENT '记录编码',
    parent_id BIGINT DEFAULT NULL COMMENT '父级菜单ID',
    title VARCHAR(64) NOT NULL COMMENT '标题',
    code VARCHAR(64) NOT NULL COMMENT '菜单编码',
    path VARCHAR(128) DEFAULT NULL COMMENT '路由路径',
    icon VARCHAR(64) DEFAULT NULL COMMENT '图标',
    order_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    menu_type VARCHAR(16) NOT NULL COMMENT '菜单类型',
    route_code VARCHAR(64) DEFAULT NULL COMMENT '路由权限码',
    permission_code VARCHAR(128) DEFAULT NULL COMMENT '权限编码',
    hidden_in_menu TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在菜单隐藏',
    redirect VARCHAR(128) DEFAULT NULL COMMENT '重定向路径',
    keep_alive TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否缓存页面',
    external_link VARCHAR(255) DEFAULT NULL COMMENT '外链地址',
    badge VARCHAR(64) DEFAULT NULL COMMENT '角标',
    disabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否禁用',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(64) DEFAULT NULL COMMENT '负责人',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_menu_code (code),
    UNIQUE KEY uk_sys_menu_record_code (record_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';


CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_role_menu_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关系表';

CREATE TABLE IF NOT EXISTS sys_role_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    action_code VARCHAR(128) NOT NULL COMMENT '按钮权限编码',
    action_name VARCHAR(64) NOT NULL COMMENT '按钮权限名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_role_action_role_code (role_id, action_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色按钮权限表';

CREATE TABLE IF NOT EXISTS sys_role_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    route_code VARCHAR(64) NOT NULL COMMENT '路由权限码',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_role_route_role_code (role_id, route_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色路由权限表';

-- resources/sql/V3__init_query_schema.sql
CREATE TABLE IF NOT EXISTS biz_query_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '名称',
    code VARCHAR(60) NOT NULL COMMENT '业务编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
    call_count BIGINT NOT NULL DEFAULT 0 COMMENT '调用次数',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_biz_query_record_record_code (record_code),
    UNIQUE KEY uk_biz_query_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询配置表';

-- resources/sql/V5__init_system_schema.sql
CREATE TABLE IF NOT EXISTS sys_user_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '用户名称',
    code VARCHAR(60) NOT NULL COMMENT '登录账号',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '负责人',
    description VARCHAR(160) NOT NULL COMMENT '描述',
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
    description VARCHAR(255) NOT NULL COMMENT '描述',
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

CREATE TABLE IF NOT EXISTS sys_log_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(40) NOT NULL COMMENT '日志名称',
    code VARCHAR(60) NOT NULL COMMENT '日志编码',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    owner VARCHAR(30) NOT NULL COMMENT '操作人',
    description VARCHAR(255) NOT NULL COMMENT '描述',
    log_type VARCHAR(16) NOT NULL COMMENT '日志类型',
    target VARCHAR(180) NOT NULL DEFAULT '' COMMENT '目标对象',
    ip_address VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'IP 地址',
    device_info VARCHAR(255) NOT NULL DEFAULT '' COMMENT '设备信息',
    request_info TEXT COMMENT '请求信息',
    response_info TEXT COMMENT '响应信息',
    result VARCHAR(20) NOT NULL DEFAULT '' COMMENT '结果',
    duration_ms BIGINT NOT NULL DEFAULT 0 COMMENT '耗时',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL COMMENT '原始创建时间',
    updated_at DATETIME NOT NULL COMMENT '原始更新时间',
    archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    UNIQUE KEY uk_sys_log_archive_record_code (record_code),
    KEY idx_sys_log_archive_query (log_type, status, created_at),
    KEY idx_sys_log_archive_archived_at (archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志留档表';

CREATE TABLE IF NOT EXISTS sys_email_verify_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    email VARCHAR(120) NOT NULL COMMENT '邮箱地址',
    verify_code VARCHAR(6) NOT NULL COMMENT '验证码',
    scene VARCHAR(32) NOT NULL COMMENT '业务场景',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    used_at DATETIME DEFAULT NULL COMMENT '使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_sys_email_verify_code_email_scene (email, scene)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码记录表';

-- resources/sql/V7__init_config_schema.sql
CREATE TABLE IF NOT EXISTS sys_config_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    name VARCHAR(64) NOT NULL COMMENT '配置名称',
    code VARCHAR(64) NOT NULL COMMENT '配置编码',
    config_type VARCHAR(16) NOT NULL COMMENT '配置类型',
    value_type VARCHAR(16) NOT NULL COMMENT '值类型',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '描述',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_config_record_record_code (record_code),
    UNIQUE KEY uk_sys_config_record_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置记录表';

-- extended dict/param/file schema

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    dict_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    dict_name VARCHAR(64) NOT NULL COMMENT '字典名称',
    dict_type VARCHAR(64) NOT NULL COMMENT '字典类型',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_dict_type_code (dict_code),
    UNIQUE KEY uk_sys_dict_type_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    data_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    dict_type_id BIGINT NOT NULL COMMENT '字典类型ID',
    dict_type VARCHAR(64) NOT NULL COMMENT '字典类型',
    label VARCHAR(64) NOT NULL COMMENT '字典标签',
    value VARCHAR(64) NOT NULL COMMENT '字典值',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_dict_data_code (data_code),
    UNIQUE KEY uk_sys_dict_data_type_value (dict_type, value),
    KEY idx_sys_dict_data_type (dict_type),
    KEY idx_sys_dict_data_type_id (dict_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

CREATE TABLE IF NOT EXISTS sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    config_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    config_name VARCHAR(64) NOT NULL COMMENT '参数名称',
    config_key VARCHAR(128) NOT NULL COMMENT '参数键',
    config_value VARCHAR(500) NOT NULL COMMENT '参数值',
    value_type VARCHAR(16) NOT NULL DEFAULT 'string' COMMENT '值类型',
    config_type VARCHAR(32) NOT NULL DEFAULT 'basic' COMMENT '参数类型',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_config_code (config_code),
    UNIQUE KEY uk_sys_config_key (config_key),
    KEY idx_sys_config_type (config_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

CREATE TABLE IF NOT EXISTS sys_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    file_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '服务端文件路径或对象 key',
    storage_type VARCHAR(16) NOT NULL DEFAULT 'local' COMMENT '存储类型',
    object_key VARCHAR(500) NOT NULL DEFAULT '' COMMENT '对象存储 key',
    file_url VARCHAR(500) NOT NULL DEFAULT '' COMMENT '文件访问地址',
    content_type VARCHAR(128) DEFAULT NULL COMMENT 'MIME 类型',
    extension VARCHAR(16) NOT NULL COMMENT '扩展名',
    size_bytes BIGINT NOT NULL COMMENT '文件大小',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_by VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '上传人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_file_code (file_code),
    UNIQUE KEY uk_sys_file_stored_name (stored_name),
    KEY idx_sys_file_storage_type (storage_type),
    KEY idx_sys_file_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';

CREATE TABLE IF NOT EXISTS sys_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    title VARCHAR(100) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    notice_type VARCHAR(32) NOT NULL COMMENT '公告类型',
    priority VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '优先级',
    publisher_id BIGINT DEFAULT NULL COMMENT '发布人ID',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    expire_time DATETIME DEFAULT NULL COMMENT '过期时间',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_sys_notice_status_type (status, notice_type),
    KEY idx_sys_notice_time (publish_time, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知表';

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '部门名称',
    code VARCHAR(64) NOT NULL COMMENT '部门编码',
    parent_id BIGINT DEFAULT NULL COMMENT '父级部门ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    leader VARCHAR(64) NOT NULL DEFAULT '' COMMENT '负责人',
    phone VARCHAR(32) NOT NULL DEFAULT '' COMMENT '联系电话',
    email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_department_code (code),
    KEY idx_sys_department_parent (parent_id),
    KEY idx_sys_department_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';
