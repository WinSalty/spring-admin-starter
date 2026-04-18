package com.winsalty.quickstart.dashboard.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.dashboard.service.DashboardService;
import com.winsalty.quickstart.dashboard.vo.DashboardOverviewVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台控制器。
 * 提供首页统计卡片、趋势图、分类图和状态分布所需的聚合数据。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 首页概览接口，响应体较大且可重复计算，审计日志不记录完整响应。
     */
    @AuditLog(logType = "api", code = "dashboard_overview", name = "工作台概览", recordResponse = false)
    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewVo> overview() {
        return ApiResponse.success("获取成功", dashboardService.getOverview());
    }
}
