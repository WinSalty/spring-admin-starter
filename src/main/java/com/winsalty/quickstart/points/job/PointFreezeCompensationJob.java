package com.winsalty.quickstart.points.job;

import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.service.PointCompensationService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 积分冻结补偿任务。
 * 定时取消已过期但仍处于 frozen 状态的冻结单，释放用户可用积分。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Component
@DisallowConcurrentExecution
public class PointFreezeCompensationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PointFreezeCompensationJob.class);

    private final PointCompensationService pointCompensationService;
    private final PointsProperties pointsProperties;

    public PointFreezeCompensationJob(PointCompensationService pointCompensationService,
                                      PointsProperties pointsProperties) {
        this.pointCompensationService = pointCompensationService;
        this.pointsProperties = pointsProperties;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!pointsProperties.isFreezeCompensationEnabled()) {
            log.info("point freeze compensation skipped, enabled=false");
            return;
        }
        try {
            log.info("point freeze compensation job started, fireTime={}", context.getFireTime());
            int processed = pointCompensationService.cancelExpiredFreezes();
            log.info("point freeze compensation job finished, processed={}, nextFireTime={}",
                    processed, context.getNextFireTime());
        } catch (RuntimeException exception) {
            log.error("point freeze compensation job failed, message={}", exception.getMessage());
            throw new JobExecutionException(exception);
        }
    }
}
