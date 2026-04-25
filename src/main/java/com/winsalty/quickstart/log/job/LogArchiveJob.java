package com.winsalty.quickstart.log.job;

import com.winsalty.quickstart.log.service.LogArchiveService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 日志归档 Quartz 任务。
 * 按配置周期触发日志归档服务，将过期日志迁移到留档表。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Component
@DisallowConcurrentExecution
public class LogArchiveJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(LogArchiveJob.class);

    private final LogArchiveService logArchiveService;

    public LogArchiveJob(LogArchiveService logArchiveService) {
        this.logArchiveService = logArchiveService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            // Quartz 禁止同一归档任务并发执行，避免两个调度实例同时迁移同一批日志。
            log.info("log archive job started, fireTime={}", context.getFireTime());
            logArchiveService.archiveExpiredLogs();
            log.info("log archive job finished, nextFireTime={}", context.getNextFireTime());
        } catch (RuntimeException exception) {
            log.error("log archive job failed, message={}", exception.getMessage());
            throw new JobExecutionException(exception);
        }
    }
}
