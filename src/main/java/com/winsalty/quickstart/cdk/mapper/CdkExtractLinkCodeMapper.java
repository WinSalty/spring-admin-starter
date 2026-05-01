package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import com.winsalty.quickstart.cdk.entity.CdkExtractLinkCodeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CDK 提取链接码明细数据访问接口。
 * 负责维护一个临时提取链接与多个 CDK 的关联关系。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CdkExtractLinkCodeMapper {

    @Insert("INSERT INTO cdk_extract_link_code(link_id, code_id, batch_id, sort_no) VALUES(#{linkId}, #{codeId}, #{batchId}, #{sortNo})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkExtractLinkCodeEntity entity);

    @Select("SELECT c.id, c.batch_id AS batchId, b.batch_no AS batchNo, b.batch_name AS batchName, "
            + "b.benefit_type AS benefitType, b.benefit_config AS benefitConfig, b.status AS batchStatus, "
            + "DATE_FORMAT(b.valid_from, '%Y-%m-%d %H:%i:%s') AS validFrom, DATE_FORMAT(b.valid_to, '%Y-%m-%d %H:%i:%s') AS validTo, "
            + "c.code_hash AS codeHash, c.encrypted_code AS encryptedCode, c.code_prefix AS codePrefix, c.checksum, c.status, "
            + "c.redeemed_user_id AS redeemedUserId, DATE_FORMAT(c.redeemed_at, '%Y-%m-%d %H:%i:%s') AS redeemedAt, "
            + "c.redeem_record_no AS redeemRecordNo, c.version, DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM cdk_extract_link_code lc INNER JOIN cdk_code c ON c.id = lc.code_id INNER JOIN cdk_batch b ON b.id = c.batch_id "
            + "WHERE lc.link_id = #{linkId} ORDER BY lc.sort_no ASC, lc.id ASC")
    List<CdkCodeEntity> findCodesByLinkId(@Param("linkId") Long linkId);
}
