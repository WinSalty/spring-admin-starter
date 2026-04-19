package com.winsalty.quickstart.dashboard.vo;

import lombok.Data;

/**
 * 工作台指标响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class DashboardMetricVo {

    private String key;
    private String title;
    private Long value;
    private String suffix;
    private Integer precision;
    private String trendLabel;
    private String trendValue;
    private String trendType;
}
