package com.winsalty.quickstart.log.service;

import com.winsalty.quickstart.log.dto.OperationLogRequest;

/**
 * 日志服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface LogService {

    void record(OperationLogRequest request);
}
