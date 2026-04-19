package com.winsalty.quickstart.dashboard.vo;

import lombok.Data;

/**
 * 工作台趋势点响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class DashboardTrendPointVo {

    private String date;
    private Long visits;
    private Long orders;
}
