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

    String CODE_SELECT = "SELECT id, batch_id AS batchId, code_hash AS codeHash, code_prefix AS codePrefix, checksum, status, "
            + "redeemed_user_id AS redeemedUserId, DATE_FORMAT(redeemed_at, '%Y-%m-%d %H:%i:%s') AS redeemedAt, "
            + "redeem_record_no AS redeemRecordNo, version, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_code ";

    @Insert("INSERT INTO cdk_code(batch_id, code_hash, code_prefix, checksum, status, version) VALUES(#{batchId}, #{codeHash}, #{codePrefix}, #{checksum}, #{status}, #{version})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkCodeEntity entity);

    @Select(CODE_SELECT + "WHERE code_hash = #{codeHash} LIMIT 1")
    CdkCodeEntity findByCodeHash(@Param("codeHash") String codeHash);

    @Update("UPDATE cdk_code SET status = 'redeemed', redeemed_user_id = #{userId}, redeemed_at = NOW(), redeem_record_no = #{redeemNo}, version = version + 1 WHERE id = #{id} AND status = 'active'")
    int markRedeemed(@Param("id") Long id,
                     @Param("userId") Long userId,
                     @Param("redeemNo") String redeemNo);
}
