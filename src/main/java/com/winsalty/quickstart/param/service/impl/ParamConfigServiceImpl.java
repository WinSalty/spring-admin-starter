package com.winsalty.quickstart.param.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.param.dto.ParamListRequest;
import com.winsalty.quickstart.param.dto.ParamSaveRequest;
import com.winsalty.quickstart.param.dto.ParamStatusRequest;
import com.winsalty.quickstart.param.entity.ParamConfigEntity;
import com.winsalty.quickstart.param.mapper.ParamConfigMapper;
import com.winsalty.quickstart.param.service.ParamConfigService;
import com.winsalty.quickstart.param.vo.ParamConfigVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParamConfigServiceImpl implements ParamConfigService {

    private static final Logger log = LoggerFactory.getLogger(ParamConfigServiceImpl.class);
    private static final String CONFIG_VERSION_KEY = "sa:cache:ver:param";
    private static final String CONFIG_CACHE_PREFIX = "sa:param:v";
    private static final long CACHE_TTL_SECONDS = 3600L;

    private final ParamConfigMapper paramConfigMapper;
    private final RedisCacheService redisCacheService;

    public ParamConfigServiceImpl(ParamConfigMapper paramConfigMapper, RedisCacheService redisCacheService) {
        this.paramConfigMapper = paramConfigMapper;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public PageResponse<ParamConfigVo> getPage(ParamListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<ParamConfigEntity> entities = paramConfigMapper.findPage(request.getKeyword(), request.getConfigType(), request.getStatus(), offset, pageSize);
        long total = paramConfigMapper.countPage(request.getKeyword(), request.getConfigType(), request.getStatus());
        return new PageResponse<ParamConfigVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public ParamConfigVo getDetail(String id) {
        return toVo(load(parseId(id)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParamConfigVo save(ParamSaveRequest request) {
        validateValue(request.getValueType(), request.getConfigValue());
        ParamConfigEntity duplicated = paramConfigMapper.findByKey(request.getConfigKey());
        if (StringUtils.hasText(request.getId())) {
            ParamConfigEntity existed = load(parseId(request.getId()));
            if (duplicated != null && !duplicated.getId().equals(existed.getId())) {
                throw new BusinessException(4031, "参数键已存在");
            }
            applyFields(existed, request);
            paramConfigMapper.update(existed);
            bumpVersion();
            return toVo(load(existed.getId()));
        }
        if (duplicated != null) {
            throw new BusinessException(4031, "参数键已存在");
        }
        ParamConfigEntity entity = new ParamConfigEntity();
        entity.setConfigCode("P" + System.currentTimeMillis());
        applyFields(entity, request);
        paramConfigMapper.insert(entity);
        bumpVersion();
        return toVo(load(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParamConfigVo updateStatus(ParamStatusRequest request) {
        Long id = parseId(request.getId());
        load(id);
        paramConfigMapper.updateStatus(id, request.getStatus());
        bumpVersion();
        return toVo(load(id));
    }

    @Override
    public Boolean refreshCache() {
        long version = bumpVersion();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        for (ParamConfigEntity entity : paramConfigMapper.findActiveAll()) {
            values.put(entity.getConfigKey(), resolveValue(entity.getValueType(), entity.getConfigValue()));
        }
        redisCacheService.set(CONFIG_CACHE_PREFIX + version + ":all", values, CACHE_TTL_SECONDS);
        log.info("param config cache refreshed, version={}, size={}", version, values.size());
        return Boolean.TRUE;
    }

    private void applyFields(ParamConfigEntity entity, ParamSaveRequest request) {
        entity.setConfigName(request.getConfigName());
        entity.setConfigKey(request.getConfigKey());
        entity.setConfigValue(normalizeValue(request.getValueType(), request.getConfigValue()));
        entity.setValueType(request.getValueType());
        entity.setConfigType(request.getConfigType());
        entity.setStatus(request.getStatus());
        entity.setRemark(defaultText(request.getRemark()));
    }

    private ParamConfigEntity load(Long id) {
        ParamConfigEntity entity = paramConfigMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(4042, "参数配置不存在");
        }
        return entity;
    }

    private void validateValue(String valueType, String value) {
        normalizeValue(valueType, value);
    }

    private String normalizeValue(String valueType, String value) {
        if ("boolean".equals(valueType)) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new BusinessException(4032, "布尔参数值不合法");
            }
            return String.valueOf(Boolean.valueOf(value));
        }
        if ("number".equals(valueType)) {
            try {
                return new BigDecimal(value).stripTrailingZeros().toPlainString();
            } catch (Exception exception) {
                throw new BusinessException(4033, "数字参数值不合法");
            }
        }
        return value;
    }

    private Object resolveValue(String valueType, String value) {
        if ("boolean".equals(valueType)) {
            return Boolean.valueOf(value);
        }
        if ("number".equals(valueType)) {
            return new BigDecimal(value).stripTrailingZeros();
        }
        return value;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(4001, "id 不合法");
        }
    }

    private long bumpVersion() {
        Long version = redisCacheService.increment(CONFIG_VERSION_KEY);
        if (version == null) {
            redisCacheService.set(CONFIG_VERSION_KEY, 1L, CACHE_TTL_SECONDS * 24);
            return 1L;
        }
        return version.longValue();
    }

    private List<ParamConfigVo> toVoList(List<ParamConfigEntity> entities) {
        List<ParamConfigVo> records = new ArrayList<ParamConfigVo>();
        for (ParamConfigEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private ParamConfigVo toVo(ParamConfigEntity entity) {
        ParamConfigVo vo = new ParamConfigVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setConfigName(entity.getConfigName());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigValue(resolveValue(entity.getValueType(), entity.getConfigValue()));
        vo.setValueType(entity.getValueType());
        vo.setConfigType(entity.getConfigType());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
