package com.winsalty.quickstart.infra.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 本地事务事件服务实现。
 * 当前版本以数据库 outbox 和日志发布占位实现最终一致扩展点，后续可替换为 MQ 投递。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class TransactionOutboxServiceImpl implements TransactionOutboxService {

    private static final Logger log = LoggerFactory.getLogger(TransactionOutboxServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String EVENT_NO_PREFIX = "OE";
    private static final String STATUS_PENDING = "pending";
    private static final String EMPTY_FAILURE_MESSAGE = "";
    private static final String EMPTY_JSON = "{}";
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long RETRY_DELAY_SECONDS = 60L;

    private final TransactionOutboxMapper transactionOutboxMapper;

    public TransactionOutboxServiceImpl(TransactionOutboxMapper transactionOutboxMapper) {
        this.transactionOutboxMapper = transactionOutboxMapper;
    }

    @Override
    public void createEvent(String aggregateType, String aggregateNo, String eventType, String payload) {
        TransactionOutboxEntity entity = new TransactionOutboxEntity();
        entity.setEventNo(createEventNo());
        entity.setAggregateType(aggregateType);
        entity.setAggregateNo(aggregateNo);
        entity.setEventType(eventType);
        // payload 为空时仍写入 JSON 对象，保证后续 MQ 适配器按统一格式反序列化。
        entity.setPayload(StringUtils.hasText(payload) ? payload : EMPTY_JSON);
        entity.setStatus(STATUS_PENDING);
        entity.setRetryCount(0);
        entity.setFailureMessage(EMPTY_FAILURE_MESSAGE);
        transactionOutboxMapper.insert(entity);
        log.info("transaction outbox event created, eventNo={}, aggregateType={}, aggregateNo={}, eventType={}",
                entity.getEventNo(), aggregateType, aggregateNo, eventType);
    }

    @Override
    public void processPendingEvents() {
        // 单次只拉取固定批量，避免 outbox 堆积时长事务占用连接和锁资源。
        List<TransactionOutboxEntity> events = transactionOutboxMapper.findPending(DEFAULT_BATCH_SIZE);
        for (TransactionOutboxEntity event : events) {
            try {
                // 当前以日志作为投递占位；接入 MQ 时应在此处完成真实发布后再标记成功。
                log.info("transaction outbox event processed, eventNo={}, eventType={}, aggregateNo={}",
                        event.getEventNo(), event.getEventType(), event.getAggregateNo());
                transactionOutboxMapper.markSuccess(event.getId());
            } catch (RuntimeException exception) {
                // 失败事件延迟重试，避免异常事件在同一调度周期内被频繁打满日志。
                transactionOutboxMapper.markRetry(event.getId(), RETRY_DELAY_SECONDS, exception.getMessage());
                log.error("transaction outbox event process failed, eventNo={}, message={}", event.getEventNo(), exception.getMessage());
            }
        }
    }

    private String createEventNo() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return EVENT_NO_PREFIX + LocalDateTime.now().format(DATE_TIME_FORMATTER)
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }
}
