package com.winsalty.quickstart.infra.outbox;

/**
 * 本地事务事件服务接口。
 * 业务模块在事务内写入事件，调度任务负责异步推进。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface TransactionOutboxService {

    void createEvent(String aggregateType, String aggregateNo, String eventType, String payload);

    void processPendingEvents();
}
