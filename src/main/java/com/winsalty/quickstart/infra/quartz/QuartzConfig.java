package com.winsalty.quickstart.infra.quartz;

import com.winsalty.quickstart.log.job.LogArchiveJob;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxJob;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxProperties;
import com.winsalty.quickstart.points.config.PointsProperties;
import com.winsalty.quickstart.points.job.PointFreezeCompensationJob;
import com.winsalty.quickstart.points.job.PointReconciliationJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 定时任务配置。
 * 统一声明项目级跑批任务，便于后续扩展更多批处理作业。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Configuration
@EnableConfigurationProperties(LogArchiveJobProperties.class)
public class QuartzConfig {

    /**
     * 日志归档任务定义。durable 保证没有触发器时任务明细仍可被 Scheduler 管理。
     */
    @Bean
    public JobDetail logArchiveJobDetail() {
        return JobBuilder.newJob(LogArchiveJob.class)
                .withIdentity("logArchiveJob", "systemBatch")
                .withDescription("Archive expired system logs")
                .storeDurably()
                .build();
    }

    /**
     * 日志归档 Cron 触发器，执行时间由 app.batch.log-archive.cron 配置。
     */
    @Bean
    public CronTrigger logArchiveJobTrigger(@Qualifier("logArchiveJobDetail") JobDetail logArchiveJobDetail,
                                            LogArchiveJobProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(logArchiveJobDetail)
                .withIdentity("logArchiveJobTrigger", "systemBatch")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 积分对账任务定义。对账只读取账务汇总数据，不允许并发执行。
     */
    @Bean
    public JobDetail pointReconciliationJobDetail() {
        return JobBuilder.newJob(PointReconciliationJob.class)
                .withIdentity("pointReconciliationJob", "systemBatch")
                .withDescription("Reconcile point account and ledger")
                .storeDurably()
                .build();
    }

    /**
     * 积分对账 Cron 触发器，执行时间由 app.points.reconciliation-cron 配置。
     */
    @Bean
    public CronTrigger pointReconciliationJobTrigger(
            @Qualifier("pointReconciliationJobDetail") JobDetail pointReconciliationJobDetail,
            PointsProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(pointReconciliationJobDetail)
                .withIdentity("pointReconciliationJobTrigger", "systemBatch")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getReconciliationCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 冻结单过期补偿任务定义。
     */
    @Bean
    public JobDetail pointFreezeCompensationJobDetail() {
        return JobBuilder.newJob(PointFreezeCompensationJob.class)
                .withIdentity("pointFreezeCompensationJob", "systemBatch")
                .withDescription("Cancel expired point freeze orders")
                .storeDurably()
                .build();
    }

    /**
     * 冻结单过期补偿 Cron 触发器。
     */
    @Bean
    public CronTrigger pointFreezeCompensationJobTrigger(
            @Qualifier("pointFreezeCompensationJobDetail") JobDetail pointFreezeCompensationJobDetail,
            PointsProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(pointFreezeCompensationJobDetail)
                .withIdentity("pointFreezeCompensationJobTrigger", "systemBatch")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getFreezeCompensationCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    /**
     * 本地事务事件任务定义。
     */
    @Bean
    public JobDetail transactionOutboxJobDetail() {
        return JobBuilder.newJob(TransactionOutboxJob.class)
                .withIdentity("transactionOutboxJob", "systemBatch")
                .withDescription("Process pending transaction outbox events")
                .storeDurably()
                .build();
    }

    /**
     * 本地事务事件 Cron 触发器。
     */
    @Bean
    public CronTrigger transactionOutboxJobTrigger(
            @Qualifier("transactionOutboxJobDetail") JobDetail transactionOutboxJobDetail,
            TransactionOutboxProperties properties) {
        return TriggerBuilder.newTrigger()
                .forJob(transactionOutboxJobDetail)
                .withIdentity("transactionOutboxJobTrigger", "systemBatch")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getCron())
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }
}
