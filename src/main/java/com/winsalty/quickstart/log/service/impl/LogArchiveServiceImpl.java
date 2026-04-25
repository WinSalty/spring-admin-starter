package com.winsalty.quickstart.log.service.impl;

import com.winsalty.quickstart.infra.quartz.LogArchiveJobProperties;
import com.winsalty.quickstart.log.mapper.LogMapper;
import com.winsalty.quickstart.log.service.LogArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日志归档服务实现。
 * 分批迁移过期日志，避免一次性大事务影响线上查询和写入。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class LogArchiveServiceImpl implements LogArchiveService {

    private static final Logger log = LoggerFactory.getLogger(LogArchiveServiceImpl.class);
    private static final int MIN_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 5000;

    private final LogMapper logMapper;
    private final LogArchiveJobProperties properties;
    private final TransactionTemplate transactionTemplate;

    public LogArchiveServiceImpl(LogMapper logMapper,
                                 LogArchiveJobProperties properties,
                                 TransactionTemplate transactionTemplate) {
        this.logMapper = logMapper;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void archiveExpiredLogs() {
        if (!properties.isEnabled()) {
            log.info("log archive skipped, enabled=false");
            return;
        }
        int batchSize = normalizeBatchSize(properties.getBatchSize());
        // retentionDays 至少按 1 天计算，避免误配置为 0 时归档当天正在写入的日志。
        Date cutoffTime = Date.from(LocalDateTime.now()
                .minusDays(Math.max(properties.getRetentionDays(), 1))
                .atZone(ZoneId.systemDefault())
                .toInstant());
        long pendingCount = logMapper.countArchiveCandidates(cutoffTime);
        log.info("log archive candidates counted, cutoffTime={}, pendingCount={}, batchSize={}",
                cutoffTime, pendingCount, batchSize);
        long totalArchived = 0L;
        while (true) {
            // 每一批独立事务提交，降低归档过程中对在线日志写入和查询的影响。
            ArchiveBatchResult result = transactionTemplate.execute(status -> archiveOneBatch(cutoffTime, batchSize));
            int archived = result == null ? 0 : result.getArchived();
            int deleted = result == null ? 0 : result.getDeleted();
            if (archived <= 0 && deleted <= 0) {
                break;
            }
            totalArchived += deleted;
            log.info("log archive batch moved, archived={}, deleted={}, totalArchived={}",
                    archived, deleted, totalArchived);
            if (archived < batchSize || deleted < batchSize) {
                break;
            }
        }
        log.info("log archive completed, cutoffTime={}, totalArchived={}", cutoffTime, totalArchived);
    }

    private int normalizeBatchSize(int batchSize) {
        if (batchSize < MIN_BATCH_SIZE) {
            // 批量过小会造成调度周期内频繁提交事务，统一抬到最低批量。
            return MIN_BATCH_SIZE;
        }
        return Math.min(batchSize, MAX_BATCH_SIZE);
    }

    private ArchiveBatchResult archiveOneBatch(Date cutoffTime, int batchSize) {
        // 先插入归档表再删除主表，删除数量用于统计本批真正迁移完成的记录。
        int archived = logMapper.archiveLogs(cutoffTime, batchSize);
        int deleted = logMapper.deleteArchivedLogs(cutoffTime, batchSize);
        return new ArchiveBatchResult(archived, deleted);
    }

    /**
     * 单批归档结果。
     * 记录本批插入留档表和删除主表的数量，便于日志追踪。
     * 创建日期：2026-04-23
     * author：sunshengxian
     */
    private static class ArchiveBatchResult {

        private final int archived;
        private final int deleted;

        ArchiveBatchResult(int archived, int deleted) {
            this.archived = archived;
            this.deleted = deleted;
        }

        int getArchived() {
            return archived;
        }

        int getDeleted() {
            return deleted;
        }
    }
}
