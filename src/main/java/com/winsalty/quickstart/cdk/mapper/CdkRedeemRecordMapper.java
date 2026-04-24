package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkRedeemRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * CDK 兑换记录数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface CdkRedeemRecordMapper {

    String RECORD_SELECT = "SELECT id, redeem_no AS redeemNo, user_id AS userId, batch_id AS batchId, code_id AS codeId, "
            + "benefit_type AS benefitType, benefit_snapshot AS benefitSnapshot, status, failure_code AS failureCode, "
            + "failure_message AS failureMessage, client_ip AS clientIp, user_agent_hash AS userAgentHash, trace_id AS traceId, "
            + "idempotency_key AS idempotencyKey, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_redeem_record ";

    @Insert("INSERT INTO cdk_redeem_record(redeem_no, user_id, batch_id, code_id, benefit_type, benefit_snapshot, status, failure_code, failure_message, client_ip, user_agent_hash, trace_id, idempotency_key) VALUES(#{redeemNo}, #{userId}, #{batchId}, #{codeId}, #{benefitType}, #{benefitSnapshot}, #{status}, #{failureCode}, #{failureMessage}, #{clientIp}, #{userAgentHash}, #{traceId}, #{idempotencyKey})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkRedeemRecordEntity entity);

    @Select(RECORD_SELECT + "WHERE user_id = #{userId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    CdkRedeemRecordEntity findByUserIdAndIdempotencyKey(@Param("userId") Long userId, @Param("idempotencyKey") String idempotencyKey);

    @Update("UPDATE cdk_redeem_record SET status = #{status}, benefit_snapshot = #{benefitSnapshot}, failure_code = #{failureCode}, failure_message = #{failureMessage} WHERE redeem_no = #{redeemNo}")
    int updateResult(@Param("redeemNo") String redeemNo,
                     @Param("status") String status,
                     @Param("benefitSnapshot") String benefitSnapshot,
                     @Param("failureCode") String failureCode,
                     @Param("failureMessage") String failureMessage);

    @Select({
            "<script>",
            RECORD_SELECT,
            "WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='batchId != null'>AND batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CdkRedeemRecordEntity> findPage(@Param("userId") Long userId,
                                         @Param("batchId") Long batchId,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM cdk_redeem_record WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='batchId != null'>AND batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("userId") Long userId,
                   @Param("batchId") Long batchId,
                   @Param("status") String status);
}
