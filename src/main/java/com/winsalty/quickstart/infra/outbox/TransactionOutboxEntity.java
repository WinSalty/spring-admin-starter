package com.winsalty.quickstart.infra.outbox;

import lombok.Data;

/**
 * 本地事务事件实体。
 * 记录业务事务内产生的待发布事件，供补偿任务异步处理。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class TransactionOutboxEntity {

    /** 主键ID。 */
    private Long id;
    /** 事件编号。 */
    private String eventNo;
    /** 聚合类型。 */
    private String aggregateType;
    /** 聚合编号。 */
    private String aggregateNo;
    /** 事件类型。 */
    private String eventType;
    /** 事件载荷。 */
    private String payload;
    /** 状态。 */
    private String status;
    /** 重试次数。 */
    private Integer retryCount;
    /** 下次重试时间。 */
    private String nextRetryAt;
    /** 失败原因。 */
    private String failureMessage;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
