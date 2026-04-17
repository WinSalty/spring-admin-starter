package com.winsalty.quickstart.log.service.impl;

import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.mapper.LogMapper;
import com.winsalty.quickstart.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 日志服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class LogServiceImpl implements LogService {

    private static final Logger log = LoggerFactory.getLogger(LogServiceImpl.class);

    private final LogMapper logMapper;

    public LogServiceImpl(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @Override
    public void record(OperationLogRequest request) {
        if (!StringUtils.hasText(request.getOwner())) {
            request.setOwner("system");
        }
        if (!StringUtils.hasText(request.getResult())) {
            request.setResult("成功");
        }
        if (request.getDurationMs() == null) {
            request.setDurationMs(0L);
        }
        String recordCode = "L" + System.currentTimeMillis();
        logMapper.insertLog(recordCode, request);
        log.info("operation log recorded, type={}, owner={}, code={}", request.getLogType(), request.getOwner(), request.getCode());
    }
}
