package com.winsalty.quickstart.points.job;

import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointReconciliationVo;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 积分对账 Quartz 任务。
 * 按配置周期汇总账户余额和流水差异，为日终账务巡检提供自动化入口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Component
@DisallowConcurrentExecution
public class PointReconciliationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PointReconciliationJob.class);

    private final PointAccountService pointAccountService;
    private final PointsProperties pointsProperties;

    public PointReconciliationJob(PointAccountService pointAccountService,
                                  PointsProperties pointsProperties) {
        this.pointAccountService = pointAccountService;
        this.pointsProperties = pointsProperties;
    }

    /**
     * 执行积分对账任务。任务关闭时仅记录跳过日志，不触发数据库查询。
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!pointsProperties.isReconciliationEnabled()) {
            log.info("points reconciliation job skipped, enabled=false");
            return;
        }
        try {
            log.info("points reconciliation job started, fireTime={}", context.getFireTime());
            // 对账任务只负责发现差异并持久化结果，不做自动调账，避免误修复扩大问题。
            PointReconciliationVo result = pointAccountService.reconcile();
            log.info("points reconciliation job finished, checkedAccounts={}, differentAccounts={}, availableDiff={}, frozenDiff={}, nextFireTime={}",
                    result.getCheckedAccounts(), result.getDifferentAccounts(), result.getTotalAvailableDiff(),
                    result.getTotalFrozenDiff(), context.getNextFireTime());
        } catch (RuntimeException exception) {
            log.error("points reconciliation job failed, message={}", exception.getMessage());
            throw new JobExecutionException(exception);
        }
    }
}
