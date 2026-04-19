package com.winsalty.quickstart.dashboard.service.impl;

import com.winsalty.quickstart.dashboard.service.DashboardService;
import com.winsalty.quickstart.dashboard.vo.DashboardCategoryVo;
import com.winsalty.quickstart.dashboard.vo.DashboardMetricVo;
import com.winsalty.quickstart.dashboard.vo.DashboardOverviewVo;
import com.winsalty.quickstart.dashboard.vo.DashboardStatusVo;
import com.winsalty.quickstart.dashboard.vo.DashboardTrendPointVo;
import com.winsalty.quickstart.system.entity.SystemRecordEntity;
import com.winsalty.quickstart.system.mapper.SystemMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台服务实现。
 * 当前从系统管理的用户、角色、字典、日志数据中聚合看板指标。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final SystemMapper systemMapper;

    public DashboardServiceImpl(SystemMapper systemMapper) {
        this.systemMapper = systemMapper;
    }

    /**
     * 组装前端 DashboardOverview 数据结构。后续接入真实业务统计时可替换各 build 方法。
     */
    @Override
    public DashboardOverviewVo getOverview() {
        log.info("dashboard overview loaded");
        DashboardOverviewVo response = new DashboardOverviewVo();
        // 工作台只展示概览，限制每类最多 200 条，避免看板接口扫描过大数据集。
        List<SystemRecordEntity> users = systemMapper.findPage("users", null, null, null, 0, 200);
        List<SystemRecordEntity> roles = systemMapper.findPage("roles", null, null, null, 0, 200);
        List<SystemRecordEntity> dicts = systemMapper.findPage("dicts", null, null, null, 0, 200);
        List<SystemRecordEntity> logs = systemMapper.findPage("logs", null, null, null, 0, 200);
        response.setMetrics(buildMetrics(users, roles, dicts, logs));
        response.setTrend(buildTrend(logs));
        response.setCategories(buildCategories(users, roles, dicts, logs));
        response.setStatusDistribution(buildStatusDistribution(users, logs));
        return response;
    }

    /**
     * 构建顶部指标卡片，保持 key 与前端图表配置稳定。
     */
    private List<DashboardMetricVo> buildMetrics(List<SystemRecordEntity> users,
                                                 List<SystemRecordEntity> roles,
                                                 List<SystemRecordEntity> dicts,
                                                 List<SystemRecordEntity> logs) {
        List<DashboardMetricVo> metrics = new ArrayList<DashboardMetricVo>();
        metrics.add(buildMetric("users", "有效用户", (long) users.size(), "人", null, "启用用户", String.valueOf(countByStatus(users, "active")), "up"));
        metrics.add(buildMetric("roles", "角色数量", (long) roles.size(), "个", null, "启用角色", String.valueOf(countByStatus(roles, "active")), "stable"));
        metrics.add(buildMetric("dicts", "字典数量", (long) dicts.size(), "个", null, "启用字典", String.valueOf(countByStatus(dicts, "active")), "up"));
        metrics.add(buildMetric("logs", "审计日志", (long) logs.size(), "条", null, "成功日志", String.valueOf(countSuccessLogs(logs)), countFailureLogs(logs) > 0 ? "down" : "up"));
        return metrics;
    }

    private DashboardMetricVo buildMetric(String key,
                                          String title,
                                          Long value,
                                          String suffix,
                                          Integer precision,
                                          String trendLabel,
                                          String trendValue,
                                          String trendType) {
        DashboardMetricVo metric = new DashboardMetricVo();
        metric.setKey(key);
        metric.setTitle(title);
        metric.setValue(value);
        metric.setSuffix(suffix);
        metric.setPrecision(precision);
        metric.setTrendLabel(trendLabel);
        metric.setTrendValue(trendValue);
        metric.setTrendType(trendType);
        return metric;
    }

    private List<DashboardTrendPointVo> buildTrend(List<SystemRecordEntity> logs) {
        List<DashboardTrendPointVo> trend = new ArrayList<DashboardTrendPointVo>();
        int size = Math.min(logs.size(), 7);
        for (int index = size - 1; index >= 0; index--) {
            SystemRecordEntity entity = logs.get(index);
            // 日志按最近记录逆序取 7 条，再倒序插入，前端折线图呈现从旧到新的趋势。
            String date = entity.getCreatedAt() == null || entity.getCreatedAt().length() < 10 ? "未知" : entity.getCreatedAt().substring(5, 10);
            trend.add(buildTrendPoint(date, safeLong(entity.getDurationMs()), "成功".equals(entity.getResult()) ? 1L : 0L));
        }
        if (trend.isEmpty()) {
            // 空数据时给一个零点，避免 ECharts 因空数组出现无坐标轴的空白体验。
            trend.add(buildTrendPoint("暂无", 0L, 0L));
        }
        return trend;
    }

    /**
     * 使用最近日志的日期和耗时构造趋势数据，避免空数据时前端图表无坐标轴。
     */
    private DashboardTrendPointVo buildTrendPoint(String date, Long visits, Long orders) {
        DashboardTrendPointVo point = new DashboardTrendPointVo();
        point.setDate(date);
        point.setVisits(visits);
        point.setOrders(orders);
        return point;
    }

    /**
     * 构造模块使用排行，当前以各模块记录数量作为排行值。
     */
    private List<DashboardCategoryVo> buildCategories(List<SystemRecordEntity> users,
                                                      List<SystemRecordEntity> roles,
                                                      List<SystemRecordEntity> dicts,
                                                      List<SystemRecordEntity> logs) {
        List<DashboardCategoryVo> categories = new ArrayList<DashboardCategoryVo>();
        categories.add(buildCategory("用户管理", (long) users.size()));
        categories.add(buildCategory("角色管理", (long) roles.size()));
        categories.add(buildCategory("字典管理", (long) dicts.size()));
        categories.add(buildCategory("日志管理", (long) logs.size()));
        return categories;
    }

    private DashboardCategoryVo buildCategory(String name, Long value) {
        DashboardCategoryVo category = new DashboardCategoryVo();
        category.setName(name);
        category.setValue(value);
        return category;
    }

    private List<DashboardStatusVo> buildStatusDistribution(List<SystemRecordEntity> users,
                                                            List<SystemRecordEntity> logs) {
        List<DashboardStatusVo> statusDistribution = new ArrayList<DashboardStatusVo>();
        statusDistribution.add(buildStatus("启用", countByStatus(users, "active")));
        statusDistribution.add(buildStatus("停用", countByStatus(users, "disabled")));
        statusDistribution.add(buildStatus("成功日志", countSuccessLogs(logs)));
        statusDistribution.add(buildStatus("失败日志", countFailureLogs(logs)));
        return statusDistribution;
    }

    private DashboardStatusVo buildStatus(String name, Long value) {
        DashboardStatusVo status = new DashboardStatusVo();
        status.setName(name);
        status.setValue(value);
        return status;
    }

    /**
     * 统计指定状态记录数，输入列表来自不同系统模块，因此只依赖通用 status 字段。
     */
    private Long countByStatus(List<SystemRecordEntity> records, String status) {
        long count = 0L;
        for (SystemRecordEntity record : records) {
            if (status.equals(record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    private Long countSuccessLogs(List<SystemRecordEntity> logs) {
        long count = 0L;
        for (SystemRecordEntity logRecord : logs) {
            if ("成功".equals(logRecord.getResult())) {
                count++;
            }
        }
        return count;
    }

    private Long countFailureLogs(List<SystemRecordEntity> logs) {
        long count = 0L;
        for (SystemRecordEntity logRecord : logs) {
            if ("失败".equals(logRecord.getResult()) || "拒绝".equals(logRecord.getResult())) {
                count++;
            }
        }
        return count;
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
