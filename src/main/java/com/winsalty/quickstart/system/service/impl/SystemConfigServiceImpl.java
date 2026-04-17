package com.winsalty.quickstart.system.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.system.dto.SystemConfigSaveRequest;
import com.winsalty.quickstart.system.entity.SystemConfigEntity;
import com.winsalty.quickstart.system.mapper.SystemConfigMapper;
import com.winsalty.quickstart.system.service.SystemConfigService;
import com.winsalty.quickstart.system.vo.SystemConfigVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统配置服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);
    private static final String CONFIG_VERSION_KEY = "sa:cache:ver:config";
    private static final String CONFIG_CACHE_KEY_PREFIX = "sa:config:v";
    private static final long CONFIG_CACHE_TTL_SECONDS = 3600L;

    private final SystemConfigMapper systemConfigMapper;
    private final RedisCacheService redisCacheService;

    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper,
                                   RedisCacheService redisCacheService) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisCacheService = redisCacheService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SystemConfigVo> getConfigs() {
        long version = currentVersion(CONFIG_VERSION_KEY);
        String cacheKey = CONFIG_CACHE_KEY_PREFIX + version + ":list";
        Object cached = redisCacheService.get(cacheKey);
        if (cached instanceof List) {
            log.info("system config cache hit, cacheKey={}, size={}", cacheKey, ((List<?>) cached).size());
            return (List<SystemConfigVo>) cached;
        }
        List<SystemConfigEntity> entities = systemConfigMapper.findAll();
        List<SystemConfigVo> records = toVoList(entities);
        redisCacheService.set(cacheKey, records, CONFIG_CACHE_TTL_SECONDS);
        log.info("system config cache refreshed, cacheKey={}, size={}", cacheKey, records.size());
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfigVo saveConfig(SystemConfigSaveRequest request) {
        SystemConfigEntity entity = systemConfigMapper.findByRecordCode(request.getId());
        if (entity == null) {
            throw new BusinessException(4042, "系统配置不存在");
        }
        String configValue = normalizeValue(entity.getValueType(), request.getValue());
        systemConfigMapper.updateValue(entity.getId(), configValue);
        entity = systemConfigMapper.findByRecordCode(request.getId());
        long version = nextVersion(CONFIG_VERSION_KEY);
        log.info("system config updated, id={}, code={}, cacheVersion={}", entity.getRecordCode(), entity.getCode(), version);
        return toVo(entity);
    }

    private long currentVersion(String versionKey) {
        Object cached = redisCacheService.get(versionKey);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        redisCacheService.set(versionKey, 1L, CONFIG_CACHE_TTL_SECONDS * 24);
        return 1L;
    }

    private long nextVersion(String versionKey) {
        Long version = redisCacheService.increment(versionKey);
        return version == null ? 1L : version.longValue();
    }

    private String normalizeValue(String valueType, Object value) {
        if ("boolean".equals(valueType)) {
            if (!(value instanceof Boolean)) {
                throw new BusinessException(4017, "布尔配置值不合法");
            }
            return String.valueOf(value);
        }
        if ("number".equals(valueType)) {
            if (value instanceof Number) {
                return new BigDecimal(String.valueOf(value)).stripTrailingZeros().toPlainString();
            }
            throw new BusinessException(4018, "数字配置值不合法");
        }
        return String.valueOf(value);
    }

    private List<SystemConfigVo> toVoList(List<SystemConfigEntity> entities) {
        List<SystemConfigVo> records = new ArrayList<SystemConfigVo>();
        for (SystemConfigEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private SystemConfigVo toVo(SystemConfigEntity entity) {
        SystemConfigVo vo = new SystemConfigVo();
        vo.setId(entity.getRecordCode());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setType(entity.getConfigType());
        vo.setValue(resolveValue(entity.getValueType(), entity.getConfigValue()));
        vo.setDescription(entity.getDescription());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Object resolveValue(String valueType, String configValue) {
        if ("boolean".equals(valueType)) {
            return Boolean.valueOf(configValue);
        }
        if ("number".equals(valueType)) {
            return Integer.valueOf(configValue);
        }
        return configValue;
    }
}
