CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    record_code VARCHAR(32) NOT NULL COMMENT '展示编号',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    email VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码密文',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
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
