package com.winsalty.quickstart.infra.outbox;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 本地事务事件数据访问接口。
 * 支持事件写入、待处理扫描和状态推进。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface TransactionOutboxMapper {

    String OUTBOX_SELECT = "SELECT id, event_no AS eventNo, aggregate_type AS aggregateType, aggregate_no AS aggregateNo, "
            + "event_type AS eventType, payload, status, retry_count AS retryCount, DATE_FORMAT(next_retry_at, '%Y-%m-%d %H:%i:%s') AS nextRetryAt, "
            + "failure_message AS failureMessage, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM transaction_outbox ";

    @Insert("INSERT INTO transaction_outbox(event_no, aggregate_type, aggregate_no, event_type, payload, status, retry_count, next_retry_at, failure_message) VALUES(#{eventNo}, #{aggregateType}, #{aggregateNo}, #{eventType}, #{payload}, #{status}, #{retryCount}, NOW(), #{failureMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TransactionOutboxEntity entity);

    @Select(OUTBOX_SELECT + "WHERE status = 'pending' AND next_retry_at <= NOW() ORDER BY id ASC LIMIT #{limit}")
    List<TransactionOutboxEntity> findPending(@Param("limit") int limit);

    @Update("UPDATE transaction_outbox SET status = 'success' WHERE id = #{id} AND status = 'pending'")
    int markSuccess(@Param("id") Long id);

    @Update("UPDATE transaction_outbox SET retry_count = retry_count + 1, next_retry_at = DATE_ADD(NOW(), INTERVAL #{retryDelaySeconds} SECOND), failure_message = #{failureMessage} WHERE id = #{id} AND status = 'pending'")
    int markRetry(@Param("id") Long id,
                  @Param("retryDelaySeconds") long retryDelaySeconds,
                  @Param("failureMessage") String failureMessage);
}
