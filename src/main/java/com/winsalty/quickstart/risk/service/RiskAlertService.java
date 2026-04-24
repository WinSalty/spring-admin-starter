package com.winsalty.quickstart.risk.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.risk.dto.RiskAlertListRequest;
import com.winsalty.quickstart.risk.vo.RiskAlertVo;

/**
 * 风险告警服务接口。
 * 负责创建运营风控告警并提供管理端查询能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface RiskAlertService {

    void createAlert(String alertType,
                     String riskLevel,
                     String subjectType,
                     String subjectNo,
                     Long userId,
                     String detailSnapshot);

    PageResponse<RiskAlertVo> listAlerts(RiskAlertListRequest request);
}
