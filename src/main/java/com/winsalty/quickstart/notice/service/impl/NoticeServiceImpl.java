package com.winsalty.quickstart.notice.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.constant.SystemConstants;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.notice.dto.NoticeListRequest;
import com.winsalty.quickstart.notice.dto.NoticeSaveRequest;
import com.winsalty.quickstart.notice.dto.NoticeStatusRequest;
import com.winsalty.quickstart.notice.entity.NoticeEntity;
import com.winsalty.quickstart.notice.mapper.NoticeMapper;
import com.winsalty.quickstart.notice.service.NoticeService;
import com.winsalty.quickstart.notice.vo.NoticeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 公告通知服务实现。
 * 管理公告的分页、详情、保存、状态切换，并提供工作台生效公告查询。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class NoticeServiceImpl extends BaseService implements NoticeService {

    private static final Logger log = LoggerFactory.getLogger(NoticeServiceImpl.class);
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final String EMPTY_TEXT = "";

    private final NoticeMapper noticeMapper;

    public NoticeServiceImpl(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    /**
     * 公告分页查询，默认页码和页大小与前端列表保持一致。
     */
    @Override
    public PageResponse<NoticeVo> getPage(NoticeListRequest request) {
        int pageNo = request.getPageNo() == null ? DEFAULT_PAGE_NO : request.getPageNo();
        int pageSize = request.getPageSize() == null ? DEFAULT_PAGE_SIZE : request.getPageSize();
        // offset 使用规范化后的分页参数，避免 mapper 接收 null 或前端默认值漂移。
        int offset = (pageNo - DEFAULT_PAGE_NO) * pageSize;
        List<NoticeEntity> entities = noticeMapper.findPage(request.getKeyword(), request.getStatus(), request.getNoticeType(), offset, pageSize);
        long total = noticeMapper.countPage(request.getKeyword(), request.getStatus(), request.getNoticeType());
        log.info("notice page loaded, pageNo={}, pageSize={}, total={}", pageNo, pageSize, total);
        return new PageResponse<NoticeVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public NoticeVo getDetail(String id) {
        NoticeEntity entity = load(parseId(id));
        log.info("notice detail loaded, id={}", id);
        return toVo(entity);
    }

    /**
     * 保存公告。未显式传 publisherId 时使用当前登录用户作为发布人。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVo save(NoticeSaveRequest request) {
        NoticeEntity entity = StringUtils.hasText(request.getId()) ? load(parseId(request.getId())) : new NoticeEntity();
        applyFields(entity, request);
        if (entity.getPublisherId() == null) {
            // 后台手工创建公告时默认用当前登录用户作为发布人，避免公告列表出现无发布人数据。
            entity.setPublisherId(currentUserId());
        }
        if (entity.getId() == null) {
            noticeMapper.insert(entity);
            log.info("notice created, id={}, title={}", entity.getId(), entity.getTitle());
        } else {
            noticeMapper.update(entity);
            log.info("notice updated, id={}, title={}", entity.getId(), entity.getTitle());
        }
        return toVo(load(entity.getId()));
    }

    /**
     * 更新公告状态，例如启用、禁用或下线。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NoticeVo updateStatus(NoticeStatusRequest request) {
        Long id = parseId(request.getId());
        NoticeEntity existed = load(id);
        noticeMapper.updateStatus(id, request.getStatus());
        log.info("notice status updated, id={}, status={}", id, request.getStatus());
        return toVo(load(id));
    }

    /**
     * 查询当前生效公告，供工作台轻量展示。
     */
    @Override
    public List<NoticeVo> getActiveNotices() {
        List<NoticeEntity> entities = noticeMapper.findActive();
        // active 查询由 SQL 负责过滤状态和有效期，service 只做协议转换，保持工作台接口轻量。
        log.info("active notices loaded, size={}", entities.size());
        return toVoList(entities);
    }

    /**
     * 保存公告实体字段，排序为空时默认 0。
     */
    private void applyFields(NoticeEntity entity, NoticeSaveRequest request) {
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        entity.setNoticeType(request.getNoticeType());
        entity.setPriority(request.getPriority());
        entity.setPublisherId(request.getPublisherId());
        entity.setPublishTime(request.getPublishTime());
        entity.setExpireTime(request.getExpireTime());
        entity.setStatus(request.getStatus());
        // sortOrder 空值按 0 处理，SQL 可直接按数值排序，不需要额外处理 null。
        entity.setSortOrder(request.getSortOrder() == null ? DEFAULT_SORT_ORDER : request.getSortOrder());
    }

    private NoticeEntity load(Long id) {
        NoticeEntity entity = noticeMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
    }

    private List<NoticeVo> toVoList(List<NoticeEntity> entities) {
        List<NoticeVo> records = new ArrayList<NoticeVo>();
        for (NoticeEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private NoticeVo toVo(NoticeEntity entity) {
        NoticeVo vo = new NoticeVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setTitle(entity.getTitle());
        vo.setContent(entity.getContent());
        vo.setNoticeType(entity.getNoticeType());
        vo.setPriority(entity.getPriority());
        vo.setPublisherId(entity.getPublisherId() == null ? EMPTY_TEXT : String.valueOf(entity.getPublisherId()));
        vo.setPublisherName(entity.getPublisherName());
        vo.setPublishTime(entity.getPublishTime());
        vo.setExpireTime(entity.getExpireTime());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
