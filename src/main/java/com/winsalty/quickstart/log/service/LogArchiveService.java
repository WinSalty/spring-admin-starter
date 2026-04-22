package com.winsalty.quickstart.log.service;

/**
 * 日志归档服务接口。
 * 将超过保留期的日志从查询主表迁移到留档表。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface LogArchiveService {

    void archiveExpiredLogs();
}
