package com.winsalty.quickstart.query.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.query.dto.QueryListRequest;
import com.winsalty.quickstart.query.dto.QuerySaveRequest;
import com.winsalty.quickstart.query.entity.QueryRecordEntity;
import com.winsalty.quickstart.query.mapper.QueryMapper;
import com.winsalty.quickstart.query.service.QueryService;
import com.winsalty.quickstart.query.vo.QueryRecordVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询配置服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class QueryServiceImpl implements QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryServiceImpl.class);

    private final QueryMapper queryMapper;

    public QueryServiceImpl(QueryMapper queryMapper) {
        this.queryMapper = queryMapper;
    }

    @Override
    public PageResponse<QueryRecordVo> getPage(QueryListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<QueryRecordEntity> entities = queryMapper.findPage(request.getKeyword(), request.getStatus(), offset, pageSize);
        long total = queryMapper.countPage(request.getKeyword(), request.getStatus());
        log.info("query page loaded, pageNo={}, pageSize={}, total={}", pageNo, pageSize, total);
        return new PageResponse<QueryRecordVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public QueryRecordVo getDetail(String id) {
        QueryRecordEntity entity = queryMapper.findByRecordCode(id);
        if (entity == null) {
            throw new BusinessException(4041, "查询配置不存在");
        }
        log.info("query detail loaded, id={}", id);
        return toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QueryRecordVo save(QuerySaveRequest request) {
        QueryRecordEntity duplicated = queryMapper.findByCode(request.getCode());
        if (StringUtils.hasText(request.getId())) {
            QueryRecordEntity existed = queryMapper.findByRecordCode(request.getId());
            if (existed == null) {
                throw new BusinessException(4041, "查询配置不存在");
            }
            if (duplicated != null && !duplicated.getId().equals(existed.getId())) {
                throw new BusinessException(4008, "查询编码已存在");
            }
            existed.setName(request.getName());
            existed.setCode(request.getCode());
            existed.setStatus(request.getStatus());
            existed.setOwner(request.getOwner());
            existed.setDescription(request.getDescription());
            queryMapper.update(existed);
            log.info("query record updated, id={}, code={}", existed.getRecordCode(), existed.getCode());
            return toVo(queryMapper.findByRecordCode(existed.getRecordCode()));
        }

        if (duplicated != null) {
            throw new BusinessException(4008, "查询编码已存在");
        }
        QueryRecordEntity entity = new QueryRecordEntity();
        entity.setRecordCode(nextRecordCode());
        entity.setName(request.getName());
        entity.setCode(request.getCode());
        entity.setStatus(request.getStatus());
        entity.setOwner(request.getOwner());
        entity.setDescription(request.getDescription());
        entity.setCallCount(0L);
        queryMapper.insert(entity);
        log.info("query record created, id={}, code={}", entity.getRecordCode(), entity.getCode());
        return toVo(queryMapper.findByRecordCode(entity.getRecordCode()));
    }

    private String nextRecordCode() {
        return "Q" + System.currentTimeMillis();
    }

    private List<QueryRecordVo> toVoList(List<QueryRecordEntity> entities) {
        List<QueryRecordVo> records = new ArrayList<QueryRecordVo>();
        for (QueryRecordEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private QueryRecordVo toVo(QueryRecordEntity entity) {
        QueryRecordVo vo = new QueryRecordVo();
        vo.setId(entity.getRecordCode());
        vo.setName(entity.getName());
        vo.setCode(entity.getCode());
        vo.setStatus(entity.getStatus());
        vo.setOwner(entity.getOwner());
        vo.setDescription(entity.getDescription());
        vo.setCallCount(entity.getCallCount());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
