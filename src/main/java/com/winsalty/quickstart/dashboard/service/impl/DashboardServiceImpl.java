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
    private static final int DASHBOARD_SAMPLE_LIMIT = 200;
    private static final int TREND_POINT_LIMIT = 7;
    private static final int CREATED_AT_DATE_MIN_LENGTH = 10;
    private static final int DATE_MONTH_DAY_START = 5;
    private static final int DATE_MONTH_DAY_END = 10;
    private static final String MODULE_USERS = "users";
    private static final String MODULE_ROLES = "roles";
    private static final String MODULE_DICTS = "dicts";
    private static final String MODULE_LOGS = "logs";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";
    private static final String RESULT_SUCCESS = "成功";
    private static final String RESULT_FAILED = "失败";
    private static final String RESULT_DENIED = "拒绝";
    private static final String UNKNOWN_DATE = "未知";
    private static final String EMPTY_DATE = "暂无";
    private static final long ZERO_COUNT = 0L;

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
        // 工作台只展示概览，按固定样本上限读取，避免看板接口扫描过大数据集。
        List<SystemRecordEntity> users = systemMapper.findPage(MODULE_USERS, null, null, null, 0, DASHBOARD_SAMPLE_LIMIT);
        List<SystemRecordEntity> roles = systemMapper.findPage(MODULE_ROLES, null, null, null, 0, DASHBOARD_SAMPLE_LIMIT);
        List<SystemRecordEntity> dicts = systemMapper.findPage(MODULE_DICTS, null, null, null, 0, DASHBOARD_SAMPLE_LIMIT);
        List<SystemRecordEntity> logs = systemMapper.findPage(MODULE_LOGS, null, null, null, 0, DASHBOARD_SAMPLE_LIMIT);
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
        metrics.add(buildMetric(MODULE_USERS, "有效用户", (long) users.size(), "人", null, "启用用户", String.valueOf(countByStatus(users, STATUS_ACTIVE)), "up"));
        metrics.add(buildMetric(MODULE_ROLES, "角色数量", (long) roles.size(), "个", null, "启用角色", String.valueOf(countByStatus(roles, STATUS_ACTIVE)), "stable"));
        metrics.add(buildMetric(MODULE_DICTS, "字典数量", (long) dicts.size(), "个", null, "启用字典", String.valueOf(countByStatus(dicts, STATUS_ACTIVE)), "up"));
        metrics.add(buildMetric(MODULE_LOGS, "审计日志", (long) logs.size(), "条", null, "成功日志", String.valueOf(countSuccessLogs(logs)), countFailureLogs(logs) > ZERO_COUNT ? "down" : "up"));
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
        int size = Math.min(logs.size(), TREND_POINT_LIMIT);
        for (int index = size - 1; index >= 0; index--) {
            SystemRecordEntity entity = logs.get(index);
            // 日志按最近记录逆序取固定数量，再倒序插入，前端折线图呈现从旧到新的趋势。
            String date = entity.getCreatedAt() == null || entity.getCreatedAt().length() < CREATED_AT_DATE_MIN_LENGTH
                    ? UNKNOWN_DATE
                    : entity.getCreatedAt().substring(DATE_MONTH_DAY_START, DATE_MONTH_DAY_END);
            trend.add(buildTrendPoint(date, safeLong(entity.getDurationMs()), RESULT_SUCCESS.equals(entity.getResult()) ? 1L : ZERO_COUNT));
        }
        if (trend.isEmpty()) {
            // 空数据时给一个零点，避免 ECharts 因空数组出现无坐标轴的空白体验。
            trend.add(buildTrendPoint(EMPTY_DATE, ZERO_COUNT, ZERO_COUNT));
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
        statusDistribution.add(buildStatus("启用", countByStatus(users, STATUS_ACTIVE)));
        statusDistribution.add(buildStatus("停用", countByStatus(users, STATUS_DISABLED)));
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
            if (RESULT_SUCCESS.equals(logRecord.getResult())) {
                count++;
            }
        }
        return count;
    }

    private Long countFailureLogs(List<SystemRecordEntity> logs) {
        long count = 0L;
        for (SystemRecordEntity logRecord : logs) {
            if (RESULT_FAILED.equals(logRecord.getResult()) || RESULT_DENIED.equals(logRecord.getResult())) {
                count++;
            }
        }
        return count;
    }

    private Long safeLong(Long value) {
        return value == null ? ZERO_COUNT : value;
    }
}
