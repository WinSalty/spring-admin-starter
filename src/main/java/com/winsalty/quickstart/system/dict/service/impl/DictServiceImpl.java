package com.winsalty.quickstart.system.dict.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.system.dict.dto.DictDataListRequest;
import com.winsalty.quickstart.system.dict.dto.DictDataSaveRequest;
import com.winsalty.quickstart.system.dict.dto.DictStatusRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeListRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeSaveRequest;
import com.winsalty.quickstart.system.dict.entity.DictDataEntity;
import com.winsalty.quickstart.system.dict.entity.DictTypeEntity;
import com.winsalty.quickstart.system.dict.mapper.DictMapper;
import com.winsalty.quickstart.system.dict.service.DictService;
import com.winsalty.quickstart.system.dict.vo.DictDataVo;
import com.winsalty.quickstart.system.dict.vo.DictTypeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 新版字典服务实现。
 * 负责字典类型、字典项维护，以及按版本号控制的字典项缓存。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Service
public class DictServiceImpl implements DictService {

    private static final Logger log = LoggerFactory.getLogger(DictServiceImpl.class);
    private static final String DICT_VERSION_KEY = "sa:cache:ver:dict:new";
    private static final String DICT_CACHE_PREFIX = "sa:dict:new:v";
    private static final long CACHE_TTL_SECONDS = 3600L;

    private final DictMapper dictMapper;
    private final RedisCacheService redisCacheService;

    public DictServiceImpl(DictMapper dictMapper, RedisCacheService redisCacheService) {
        this.dictMapper = dictMapper;
        this.redisCacheService = redisCacheService;
    }

    /**
     * 查询字典类型分页列表。
     */
    @Override
    public PageResponse<DictTypeVo> getTypePage(DictTypeListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<DictTypeEntity> entities = dictMapper.findTypePage(request.getKeyword(), request.getStatus(), offset, pageSize);
        long total = dictMapper.countTypePage(request.getKeyword(), request.getStatus());
        log.info("dict type page loaded, keyword={}, status={}, pageNo={}, pageSize={}, total={}",
                request.getKeyword(), request.getStatus(), pageNo, pageSize, total);
        return new PageResponse<DictTypeVo>(toTypeVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 保存字典类型。编辑类型编码时，同步更新已有字典项的 dictType。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictTypeVo saveType(DictTypeSaveRequest request) {
        DictTypeEntity duplicated = dictMapper.findTypeByDictType(request.getDictType());
        if (StringUtils.hasText(request.getId())) {
            DictTypeEntity existed = loadType(parseId(request.getId()));
            if (duplicated != null && !duplicated.getId().equals(existed.getId())) {
                throw new BusinessException(4021, "字典类型已存在");
            }
            String oldDictType = existed.getDictType();
            existed.setDictName(request.getDictName());
            existed.setDictType(request.getDictType());
            existed.setStatus(request.getStatus());
            existed.setRemark(defaultText(request.getRemark()));
            dictMapper.updateType(existed);
            if (!oldDictType.equals(request.getDictType())) {
                // 类型编码变更时同步已有字典项，否则旧数据会挂在不可见的 dictType 下。
                dictMapper.updateDataDictType(oldDictType, request.getDictType(), existed.getId());
            }
            long version = bumpVersion();
            log.info("dict type updated, id={}, dictType={}, oldDictType={}, cacheVersion={}",
                    existed.getId(), existed.getDictType(), oldDictType, version);
            return toTypeVo(loadType(existed.getId()));
        }
        if (duplicated != null) {
            throw new BusinessException(4021, "字典类型已存在");
        }
        DictTypeEntity entity = new DictTypeEntity();
        entity.setDictCode("DT" + System.currentTimeMillis());
        entity.setDictName(request.getDictName());
        entity.setDictType(request.getDictType());
        entity.setStatus(request.getStatus());
        entity.setRemark(defaultText(request.getRemark()));
        dictMapper.insertType(entity);
        long version = bumpVersion();
        log.info("dict type created, id={}, dictType={}, cacheVersion={}",
                entity.getId(), entity.getDictType(), version);
        return toTypeVo(loadType(entity.getId()));
    }

    /**
     * 切换字典类型状态并刷新字典缓存版本。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictTypeVo updateTypeStatus(DictStatusRequest request) {
        Long id = parseId(request.getId());
        DictTypeEntity entity = loadType(id);
        dictMapper.updateTypeStatus(id, request.getStatus());
        long version = bumpVersion();
        log.info("dict type status updated, id={}, dictType={}, status={}, cacheVersion={}",
                id, entity.getDictType(), request.getStatus(), version);
        return toTypeVo(loadType(id));
    }

    /**
     * 字典项分页查询。无关键字/状态筛选时，按 dictType 缓存启用项。
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageResponse<DictDataVo> getDataPage(DictDataListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        if (StringUtils.hasText(request.getDictType()) && !StringUtils.hasText(request.getKeyword()) && !StringUtils.hasText(request.getStatus())) {
            String cacheKey = DICT_CACHE_PREFIX + currentVersion() + ":" + request.getDictType();
            Object cached = redisCacheService.get(cacheKey);
            List<DictDataVo> records;
            if (cached instanceof List) {
                records = (List<DictDataVo>) cached;
                log.info("dict data cache hit, dictType={}, pageNo={}, pageSize={}, size={}",
                        request.getDictType(), pageNo, pageSize, records.size());
            } else {
                // 常见业务只按 dictType 拉启用字典项，缓存该路径可以覆盖绝大多数前端下拉框。
                records = toDataVoList(dictMapper.findActiveDataByType(request.getDictType()));
                redisCacheService.set(cacheKey, records, CACHE_TTL_SECONDS);
                log.info("dict data cache refreshed, dictType={}, cacheKey={}, size={}",
                        request.getDictType(), cacheKey, records.size());
            }
            int fromIndex = Math.min((pageNo - 1) * pageSize, records.size());
            int toIndex = Math.min(fromIndex + pageSize, records.size());
            return new PageResponse<DictDataVo>(records.subList(fromIndex, toIndex), pageNo, pageSize, records.size());
        }
        int offset = (pageNo - 1) * pageSize;
        List<DictDataEntity> entities = dictMapper.findDataPage(request.getDictType(), request.getKeyword(), request.getStatus(), offset, pageSize);
        long total = dictMapper.countDataPage(request.getDictType(), request.getKeyword(), request.getStatus());
        log.info("dict data page loaded, dictType={}, keyword={}, status={}, pageNo={}, pageSize={}, total={}",
                request.getDictType(), request.getKeyword(), request.getStatus(), pageNo, pageSize, total);
        return new PageResponse<DictDataVo>(toDataVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public DictDataVo getDataDetail(String id) {
        DictDataEntity entity = loadData(parseId(id));
        log.info("dict data detail loaded, id={}, dictType={}, value={}",
                entity.getId(), entity.getDictType(), entity.getValue());
        return toDataVo(entity);
    }

    /**
     * 保存字典项，并保证同一字典类型下 value 不重复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictDataVo saveData(DictDataSaveRequest request) {
        DictTypeEntity type = dictMapper.findTypeByDictType(request.getDictType());
        if (type == null) {
            // 字典项必须挂到已存在的字典类型，避免前端下拉框无法按类型聚合。
            throw new BusinessException(4022, "字典类型不存在");
        }
        DictDataEntity duplicated = dictMapper.findDataByTypeAndValue(request.getDictType(), request.getValue());
        if (StringUtils.hasText(request.getId())) {
            DictDataEntity existed = loadData(parseId(request.getId()));
            if (duplicated != null && !duplicated.getId().equals(existed.getId())) {
                throw new BusinessException(4023, "字典值已存在");
            }
            applyDataFields(existed, type, request);
            dictMapper.updateData(existed);
            long version = bumpVersion();
            log.info("dict data updated, id={}, dictType={}, value={}, cacheVersion={}",
                    existed.getId(), existed.getDictType(), existed.getValue(), version);
            return toDataVo(loadData(existed.getId()));
        }
        if (duplicated != null) {
            throw new BusinessException(4023, "字典值已存在");
        }
        DictDataEntity entity = new DictDataEntity();
        entity.setDataCode("DD" + System.currentTimeMillis());
        applyDataFields(entity, type, request);
        dictMapper.insertData(entity);
        long version = bumpVersion();
        log.info("dict data created, id={}, dictType={}, value={}, cacheVersion={}",
                entity.getId(), entity.getDictType(), entity.getValue(), version);
        return toDataVo(loadData(entity.getId()));
    }

    /**
     * 切换字典项状态后递增缓存版本。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictDataVo updateDataStatus(DictStatusRequest request) {
        Long id = parseId(request.getId());
        DictDataEntity entity = loadData(id);
        dictMapper.updateDataStatus(id, request.getStatus());
        long version = bumpVersion();
        log.info("dict data status updated, id={}, dictType={}, value={}, status={}, cacheVersion={}",
                id, entity.getDictType(), entity.getValue(), request.getStatus(), version);
        return toDataVo(loadData(id));
    }

    /**
     * 手动刷新缓存版本。旧缓存由 TTL 自然淘汰。
     */
    @Override
    public Boolean refreshCache() {
        long version = bumpVersion();
        log.info("dict cache refreshed, version={}", version);
        return Boolean.TRUE;
    }

    /**
     * 字典项保存时补齐类型 ID、类型编码和展示字段。
     */
    private void applyDataFields(DictDataEntity entity, DictTypeEntity type, DictDataSaveRequest request) {
        entity.setDictTypeId(type.getId());
        entity.setDictType(type.getDictType());
        // 保存时同时冗余 typeId 和 dictType，方便列表查询和外部按编码读取。
        entity.setLabel(request.getLabel());
        entity.setValue(request.getValue());
        entity.setSortNo(request.getSortNo());
        entity.setStatus(request.getStatus());
        entity.setRemark(defaultText(request.getRemark()));
    }

    private DictTypeEntity loadType(Long id) {
        DictTypeEntity entity = dictMapper.findTypeById(id);
        if (entity == null) {
            throw new BusinessException(4042, "字典类型不存在");
        }
        return entity;
    }

    private DictDataEntity loadData(Long id) {
        DictDataEntity entity = dictMapper.findDataById(id);
        if (entity == null) {
            throw new BusinessException(4042, "字典项不存在");
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(4001, "id 不合法");
        }
    }

    private long currentVersion() {
        Object version = redisCacheService.get(DICT_VERSION_KEY);
        if (version instanceof Number) {
            return ((Number) version).longValue();
        }
        // 首次访问没有版本号时初始化，后续缓存 key 才能稳定按版本拼接。
        redisCacheService.set(DICT_VERSION_KEY, 1L, CACHE_TTL_SECONDS * 24);
        return 1L;
    }

    /**
     * 递增字典缓存版本，所有按旧版本号组织的字典项缓存自动失效。
     */
    private long bumpVersion() {
        Long version = redisCacheService.increment(DICT_VERSION_KEY);
        if (version == null) {
            redisCacheService.set(DICT_VERSION_KEY, 1L, CACHE_TTL_SECONDS * 24);
            return 1L;
        }
        return version.longValue();
    }

    private List<DictTypeVo> toTypeVoList(List<DictTypeEntity> entities) {
        List<DictTypeVo> records = new ArrayList<DictTypeVo>();
        for (DictTypeEntity entity : entities) {
            records.add(toTypeVo(entity));
        }
        return records;
    }

    private DictTypeVo toTypeVo(DictTypeEntity entity) {
        DictTypeVo vo = new DictTypeVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setDictName(entity.getDictName());
        vo.setDictType(entity.getDictType());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setItemCount(dictMapper.countDataByType(entity.getDictType()));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<DictDataVo> toDataVoList(List<DictDataEntity> entities) {
        List<DictDataVo> records = new ArrayList<DictDataVo>();
        for (DictDataEntity entity : entities) {
            records.add(toDataVo(entity));
        }
        return records;
    }

    private DictDataVo toDataVo(DictDataEntity entity) {
        DictDataVo vo = new DictDataVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setDictType(entity.getDictType());
        vo.setLabel(entity.getLabel());
        vo.setValue(entity.getValue());
        vo.setSortNo(entity.getSortNo());
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
