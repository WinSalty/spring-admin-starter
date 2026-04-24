package com.winsalty.quickstart.infra.outbox;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地事务事件处理任务。
 * 定时扫描 pending outbox 事件并推进到成功或重试状态。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Component
@DisallowConcurrentExecution
public class TransactionOutboxJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TransactionOutboxJob.class);

    private final TransactionOutboxService transactionOutboxService;
    private final TransactionOutboxProperties transactionOutboxProperties;

    public TransactionOutboxJob(TransactionOutboxService transactionOutboxService,
                                TransactionOutboxProperties transactionOutboxProperties) {
        this.transactionOutboxService = transactionOutboxService;
        this.transactionOutboxProperties = transactionOutboxProperties;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (!transactionOutboxProperties.isEnabled()) {
            log.info("transaction outbox job skipped, enabled=false");
            return;
        }
        try {
            log.info("transaction outbox job started, fireTime={}", context.getFireTime());
            transactionOutboxService.processPendingEvents();
            log.info("transaction outbox job finished, nextFireTime={}", context.getNextFireTime());
        } catch (RuntimeException exception) {
            log.error("transaction outbox job failed, message={}", exception.getMessage());
            throw new JobExecutionException(exception);
        }
    }
}
