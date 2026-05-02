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
            // 没有登录态的系统任务或异常兜底统一归属 system，便于日志筛选。
            request.setOwner(SystemConstants.SYSTEM_OPERATOR);
        }
        if (!StringUtils.hasText(request.getResult())) {
            // 调用方未明确失败时按成功记录，失败场景由审计切面/异常处理器显式传入。
            request.setResult(SystemConstants.RESULT_SUCCESS);
        }
        // 数据库字段有长度限制，入库前截断可避免日志写入反向影响业务请求。
        request.setDescription(limit(request.getDescription(), 255));
        request.setTarget(limit(request.getTarget(), 180));
        request.setDeviceInfo(limit(request.getDeviceInfo(), 255));
        request.setBrowser(limit(request.getBrowser(), 64));
        request.setBrowserVersion(limit(request.getBrowserVersion(), 64));
        request.setOsName(limit(request.getOsName(), 64));
        request.setOsVersion(limit(request.getOsVersion(), 64));
        request.setDeviceType(limit(request.getDeviceType(), 32));
        request.setDeviceBrand(limit(request.getDeviceBrand(), 64));
        if (request.getDurationMs() == null) {
            request.setDurationMs(0L);
        }
        String recordCode = "L" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
        // 使用随机业务码避免并发日志记录时 System.currentTimeMillis 碰撞。
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
