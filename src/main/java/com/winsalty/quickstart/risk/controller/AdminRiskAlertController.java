package com.winsalty.quickstart.risk.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.risk.dto.RiskAlertListRequest;
import com.winsalty.quickstart.risk.service.RiskAlertService;
import com.winsalty.quickstart.risk.vo.RiskAlertVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端风险告警控制器。
 * 提供异常兑换和高价值操作告警列表查询能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/risk-alerts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRiskAlertController {

    private final RiskAlertService riskAlertService;

    public AdminRiskAlertController(RiskAlertService riskAlertService) {
        this.riskAlertService = riskAlertService;
    }

    @GetMapping
    public ApiResponse<PageResponse<RiskAlertVo>> alerts(@Validated RiskAlertListRequest request) {
        return ApiResponse.success("获取成功", riskAlertService.listAlerts(request));
    }
}
