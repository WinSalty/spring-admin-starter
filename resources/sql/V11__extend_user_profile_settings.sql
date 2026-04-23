ALTER TABLE sys_user
    ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像地址' AFTER nickname,
    ADD COLUMN country VARCHAR(64) NOT NULL DEFAULT '中国' COMMENT '国家/地区' AFTER avatar_url,
    ADD COLUMN province VARCHAR(64) DEFAULT NULL COMMENT '省份' AFTER country,
    ADD COLUMN city VARCHAR(64) DEFAULT NULL COMMENT '城市' AFTER province,
    ADD COLUMN street_address VARCHAR(255) DEFAULT NULL COMMENT '街道地址' AFTER city,
    ADD COLUMN phone_prefix VARCHAR(12) DEFAULT NULL COMMENT '电话区号' AFTER street_address,
    ADD COLUMN phone_number VARCHAR(32) DEFAULT NULL COMMENT '联系电话' AFTER phone_prefix,
    ADD COLUMN notify_account TINYINT(1) NOT NULL DEFAULT 1 COMMENT '账号安全通知' AFTER phone_number,
    ADD COLUMN notify_system TINYINT(1) NOT NULL DEFAULT 1 COMMENT '系统消息通知' AFTER notify_account,
    ADD COLUMN notify_todo TINYINT(1) NOT NULL DEFAULT 0 COMMENT '待办任务通知' AFTER notify_system;
