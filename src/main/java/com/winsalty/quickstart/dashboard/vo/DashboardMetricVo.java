package com.winsalty.quickstart.dashboard.vo;

/**
 * 工作台指标响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class DashboardMetricVo {

    private String key;
    private String title;
    private Long value;
    private String suffix;
    private Integer precision;
    private String trendLabel;
    private String trendValue;
    private String trendType;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public String getTrendLabel() {
        return trendLabel;
    }

    public void setTrendLabel(String trendLabel) {
        this.trendLabel = trendLabel;
    }

    public String getTrendValue() {
        return trendValue;
    }

    public void setTrendValue(String trendValue) {
        this.trendValue = trendValue;
    }

    public String getTrendType() {
        return trendType;
    }

    public void setTrendType(String trendType) {
        this.trendType = trendType;
    }
}
