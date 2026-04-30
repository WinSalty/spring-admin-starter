package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * CDK 码数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface CdkCodeMapper {

    String CODE_SELECT = "SELECT id, batch_id AS batchId, code_hash AS codeHash, encrypted_code AS encryptedCode, code_prefix AS codePrefix, checksum, status, "
            + "redeemed_user_id AS redeemedUserId, DATE_FORMAT(redeemed_at, '%Y-%m-%d %H:%i:%s') AS redeemedAt, "
            + "redeem_record_no AS redeemRecordNo, version, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_code ";
    String CODE_BATCH_SELECT = "SELECT c.id, c.batch_id AS batchId, b.batch_no AS batchNo, b.batch_name AS batchName, "
            + "b.benefit_type AS benefitType, b.benefit_config AS benefitConfig, b.status AS batchStatus, "
            + "DATE_FORMAT(b.valid_from, '%Y-%m-%d %H:%i:%s') AS validFrom, DATE_FORMAT(b.valid_to, '%Y-%m-%d %H:%i:%s') AS validTo, "
            + "c.code_hash AS codeHash, c.encrypted_code AS encryptedCode, c.code_prefix AS codePrefix, c.checksum, c.status, "
            + "c.redeemed_user_id AS redeemedUserId, DATE_FORMAT(c.redeemed_at, '%Y-%m-%d %H:%i:%s') AS redeemedAt, "
            + "c.redeem_record_no AS redeemRecordNo, c.version, DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_code c INNER JOIN cdk_batch b ON b.id = c.batch_id ";

    @Insert("INSERT INTO cdk_code(batch_id, code_hash, encrypted_code, code_prefix, checksum, status, version) VALUES(#{batchId}, #{codeHash}, #{encryptedCode}, #{codePrefix}, #{checksum}, #{status}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkCodeEntity entity);

    @Select(CODE_SELECT + "WHERE code_hash = #{codeHash} LIMIT 1")
    CdkCodeEntity findByCodeHash(@Param("codeHash") String codeHash);

    @Select(CODE_SELECT + "WHERE id = #{id} LIMIT 1")
    CdkCodeEntity findById(@Param("id") Long id);

    @Select(CODE_BATCH_SELECT + "WHERE c.id = #{id} LIMIT 1")
    CdkCodeEntity findWithBatchById(@Param("id") Long id);

    @Select(CODE_SELECT + "WHERE id = #{id} LIMIT 1 FOR UPDATE")
    CdkCodeEntity findByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE cdk_code SET status = 'redeemed', redeemed_user_id = #{userId}, redeemed_at = NOW(), redeem_record_no = #{redeemNo}, version = version + 1 WHERE id = #{id} AND status = 'active'")
    int markRedeemed(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("redeemNo") String redeemNo);

    @Update("UPDATE cdk_code SET status = #{status}, version = version + 1 WHERE id = #{id} AND status IN ('active', 'disabled')")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE cdk_code SET status = 'disabled', version = version + 1 WHERE batch_id = #{batchId} AND status = 'active'")
    int disableActiveByBatchId(@Param("batchId") Long batchId);

    @Select({
            "<script>",
            CODE_BATCH_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(c.code_prefix) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='batchId != null'>AND c.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND c.status = #{status} </if>",
            "ORDER BY c.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    java.util.List<CdkCodeEntity> findPage(@Param("keyword") String keyword,
                                           @Param("batchId") Long batchId,
                                           @Param("status") String status,
                                           @Param("offset") int offset,
                                           @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM cdk_code c INNER JOIN cdk_batch b ON b.id = c.batch_id WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(c.code_prefix) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='batchId != null'>AND c.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND c.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword, @Param("batchId") Long batchId, @Param("status") String status);

}
