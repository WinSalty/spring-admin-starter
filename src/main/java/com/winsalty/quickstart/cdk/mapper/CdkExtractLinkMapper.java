package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkExtractLinkEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * CDK 提取链接数据访问接口。
 * 负责临时提取 URL 的创建、状态变更、次数更新和管理端查询。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Mapper
public interface CdkExtractLinkMapper {

    String LINK_SELECT = "SELECT id, link_no AS linkNo, code_id AS codeId, batch_id AS batchId, token_hash AS tokenHash, "
            + "max_access_count AS maxAccessCount, accessed_count AS accessedCount, DATE_FORMAT(expire_at, '%Y-%m-%d %H:%i:%s') AS expireAt, "
            + "status, created_by AS createdBy, disabled_by AS disabledBy, DATE_FORMAT(disabled_at, '%Y-%m-%d %H:%i:%s') AS disabledAt, "
            + "remark, DATE_FORMAT(last_accessed_at, '%Y-%m-%d %H:%i:%s') AS lastAccessedAt, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_extract_link ";

    @Insert("INSERT INTO cdk_extract_link(link_no, code_id, batch_id, token_hash, max_access_count, accessed_count, expire_at, status, created_by, remark) "
            + "VALUES(#{linkNo}, #{codeId}, #{batchId}, #{tokenHash}, #{maxAccessCount}, #{accessedCount}, #{expireAt}, #{status}, #{createdBy}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkExtractLinkEntity entity);

    @Select(LINK_SELECT + "WHERE id = #{id} LIMIT 1")
    CdkExtractLinkEntity findById(@Param("id") Long id);

    @Select(LINK_SELECT + "WHERE id = #{id} LIMIT 1 FOR UPDATE")
    CdkExtractLinkEntity findByIdForUpdate(@Param("id") Long id);

    @Select(LINK_SELECT + "WHERE token_hash = #{tokenHash} LIMIT 1 FOR UPDATE")
    CdkExtractLinkEntity findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Select(LINK_SELECT + "WHERE code_id = #{codeId} ORDER BY id DESC")
    List<CdkExtractLinkEntity> findByCodeId(@Param("codeId") Long codeId);

    @Update("UPDATE cdk_extract_link SET accessed_count = accessed_count + 1, last_accessed_at = NOW(), "
            + "status = CASE WHEN accessed_count + 1 >= max_access_count THEN 'exhausted' ELSE status END "
            + "WHERE id = #{id} AND status = 'active' AND accessed_count < max_access_count")
    int incrementAccessed(@Param("id") Long id);

    @Update("UPDATE cdk_extract_link SET status = 'disabled', disabled_by = #{disabledBy}, disabled_at = NOW(), remark = #{remark} WHERE id = #{id} AND status = 'active'")
    int disable(@Param("id") Long id,
                @Param("disabledBy") String disabledBy,
                @Param("remark") String remark);
}
