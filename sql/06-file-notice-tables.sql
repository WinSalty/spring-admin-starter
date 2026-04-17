USE `spring_admin`;

CREATE TABLE IF NOT EXISTS `sys_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_path` varchar(512) NOT NULL COMMENT '文件路径',
  `file_size` bigint(20) NOT NULL DEFAULT '0' COMMENT '文件大小',
  `file_type` varchar(64) DEFAULT NULL COMMENT '文件类型',
  `mime_type` varchar(128) DEFAULT NULL COMMENT 'MIME类型',
  `uploader_id` bigint(20) DEFAULT NULL COMMENT '上传人ID',
  `upload_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_uploader_id` (`uploader_id`),
  KEY `idx_sys_file_upload_time` (`upload_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';

CREATE TABLE IF NOT EXISTS `sys_notice` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` varchar(128) NOT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `notice_type` varchar(32) NOT NULL DEFAULT 'notice' COMMENT '公告类型',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1发布',
  `publisher_id` bigint(20) DEFAULT NULL COMMENT '发布人',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `revoke_time` datetime DEFAULT NULL COMMENT '撤回时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_notice_status_publish_time` (`status`,`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';
