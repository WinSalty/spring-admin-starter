package com.winsalty.quickstart.risk.service.impl;

import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.risk.dto.RiskAlertListRequest;
import com.winsalty.quickstart.risk.entity.RiskAlertEntity;
import com.winsalty.quickstart.risk.mapper.RiskAlertMapper;
import com.winsalty.quickstart.risk.service.RiskAlertService;
import com.winsalty.quickstart.risk.vo.RiskAlertVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 风险告警服务实现。
 * 统一生成告警编号，避免风控写入逻辑散落在各业务模块。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class RiskAlertServiceImpl implements RiskAlertService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ALERT_NO_PREFIX = "RA";
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final RiskAlertMapper riskAlertMapper;

    public RiskAlertServiceImpl(RiskAlertMapper riskAlertMapper) {
        this.riskAlertMapper = riskAlertMapper;
    }

    @Override
    public void createAlert(String alertType,
                            String riskLevel,
                            String subjectType,
                            String subjectNo,
                            Long userId,
                            String detailSnapshot) {
        RiskAlertEntity entity = new RiskAlertEntity();
        entity.setAlertNo(createNo());
        entity.setAlertType(alertType);
        entity.setRiskLevel(riskLevel);
        entity.setSubjectType(subjectType);
        entity.setSubjectNo(subjectNo);
        entity.setUserId(userId);
        entity.setStatus(CdkConstants.ALERT_STATUS_OPEN);
        entity.setDetailSnapshot(detailSnapshot);
        riskAlertMapper.insert(entity);
    }

    @Override
    public PageResponse<RiskAlertVo> listAlerts(RiskAlertListRequest request) {
        int pageNo = normalizePageNo(request.getPageNo());
        int pageSize = normalizePageSize(request.getPageSize());
        List<RiskAlertEntity> entities = riskAlertMapper.findPage(request.getAlertType(), request.getStatus(),
                (pageNo - 1) * pageSize, pageSize);
        long total = riskAlertMapper.countPage(request.getAlertType(), request.getStatus());
        return new PageResponse<RiskAlertVo>(toVoList(entities), pageNo, pageSize, total);
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < PointsConstants.DEFAULT_PAGE_NO ? PointsConstants.DEFAULT_PAGE_NO : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return PointsConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, PointsConstants.MAX_PAGE_SIZE);
    }

    private List<RiskAlertVo> toVoList(List<RiskAlertEntity> entities) {
        List<RiskAlertVo> records = new ArrayList<RiskAlertVo>();
        for (RiskAlertEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private RiskAlertVo toVo(RiskAlertEntity entity) {
        RiskAlertVo vo = new RiskAlertVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setAlertNo(entity.getAlertNo());
        vo.setAlertType(entity.getAlertType());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setSubjectType(entity.getSubjectType());
        vo.setSubjectNo(entity.getSubjectNo());
        vo.setUserId(entity.getUserId());
        vo.setStatus(entity.getStatus());
        vo.setDetailSnapshot(entity.getDetailSnapshot());
        vo.setHandledBy(entity.getHandledBy());
        vo.setHandledAt(entity.getHandledAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String createNo() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ALERT_NO_PREFIX + LocalDateTime.now().format(DATE_TIME_FORMATTER)
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }
}
