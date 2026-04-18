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
    file_path VARCHAR(500) NOT NULL COMMENT '本地文件路径',
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
    KEY idx_sys_file_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件记录表';
