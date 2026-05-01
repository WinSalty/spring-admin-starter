package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialRedeemRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 凭证兑换记录数据访问接口。
 * 提供兑换流水写入、幂等查询和管理端分页查询能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialRedeemRecordMapper {

    String RECORD_SELECT = "SELECT id, record_no AS recordNo, item_id AS itemId, batch_id AS batchId, category_id AS categoryId, "
            + "user_id AS userId, points, idempotency_key AS idempotencyKey, client_ip AS clientIp, user_agent_hash AS userAgentHash, "
            + "device_fingerprint AS deviceFingerprint, ledger_no AS ledgerNo, status, failure_reason AS failureReason, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt FROM credential_redeem_record ";

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
    List<CredentialRedeemRecordEntity> findPage(@Param("userId") Long userId,
                                                @Param("batchId") Long batchId,
                                                @Param("status") String status,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_redeem_record WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='batchId != null'>AND batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("userId") Long userId,
                   @Param("batchId") Long batchId,
                   @Param("status") String status);

    @Select(RECORD_SELECT + "WHERE user_id = #{userId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    CredentialRedeemRecordEntity findByUserAndIdempotency(@Param("userId") Long userId,
                                                          @Param("idempotencyKey") String idempotencyKey);

    @Insert("INSERT INTO credential_redeem_record(record_no, item_id, batch_id, category_id, user_id, points, idempotency_key, client_ip, user_agent_hash, device_fingerprint, ledger_no, status, failure_reason) "
            + "VALUES(#{recordNo}, #{itemId}, #{batchId}, #{categoryId}, #{userId}, #{points}, #{idempotencyKey}, #{clientIp}, #{userAgentHash}, #{deviceFingerprint}, #{ledgerNo}, #{status}, #{failureReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialRedeemRecordEntity entity);
}
