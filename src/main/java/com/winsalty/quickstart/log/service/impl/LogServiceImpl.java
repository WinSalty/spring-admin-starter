package com.winsalty.quickstart.log.service.impl;

import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.log.dto.OperationLogRequest;
import com.winsalty.quickstart.log.mapper.LogMapper;
import com.winsalty.quickstart.log.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

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
            request.setOwner(SystemConstants.SYSTEM_OPERATOR);
        }
        if (!StringUtils.hasText(request.getResult())) {
            request.setResult(SystemConstants.RESULT_SUCCESS);
        }
        request.setDescription(limit(request.getDescription(), 255));
        request.setTarget(limit(request.getTarget(), 180));
        request.setDeviceInfo(limit(request.getDeviceInfo(), 255));
        if (request.getDurationMs() == null) {
            request.setDurationMs(0L);
        }
        String recordCode = "L" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
        logMapper.insertLog(recordCode, request);
        log.info("operation log recorded, type={}, owner={}, code={}", request.getLogType(), request.getOwner(), request.getCode());
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
