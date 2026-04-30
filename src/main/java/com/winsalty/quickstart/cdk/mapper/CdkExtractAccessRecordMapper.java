package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkExtractAccessRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CDK 提取访问记录数据访问接口。
 * 负责记录公开提取 URL 的成功和失败访问审计流水。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Mapper
public interface CdkExtractAccessRecordMapper {

    String RECORD_SELECT = "SELECT id, access_no AS accessNo, link_id AS linkId, code_id AS codeId, batch_id AS batchId, result, "
            + "failure_code AS failureCode, failure_message AS failureMessage, client_ip AS clientIp, user_agent_hash AS userAgentHash, "
            + "browser_fingerprint AS browserFingerprint, device_snapshot AS deviceSnapshot, referer, trace_id AS traceId, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt FROM cdk_extract_access_record ";

    @Insert("INSERT INTO cdk_extract_access_record(access_no, link_id, code_id, batch_id, result, failure_code, failure_message, client_ip, user_agent_hash, browser_fingerprint, device_snapshot, referer, trace_id) "
            + "VALUES(#{accessNo}, #{linkId}, #{codeId}, #{batchId}, #{result}, #{failureCode}, #{failureMessage}, #{clientIp}, #{userAgentHash}, #{browserFingerprint}, #{deviceSnapshot}, #{referer}, #{traceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkExtractAccessRecordEntity entity);

    @Select({
            "<script>",
            RECORD_SELECT,
            "WHERE link_id = #{linkId} ",
            "<if test='result != null and result != \"\"'>AND result = #{result} </if>",
            "<if test='fingerprint != null and fingerprint != \"\"'>AND browser_fingerprint = #{fingerprint} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CdkExtractAccessRecordEntity> findPage(@Param("linkId") Long linkId,
                                                @Param("result") String result,
                                                @Param("fingerprint") String fingerprint,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM cdk_extract_access_record WHERE link_id = #{linkId} ",
            "<if test='result != null and result != \"\"'>AND result = #{result} </if>",
            "<if test='fingerprint != null and fingerprint != \"\"'>AND browser_fingerprint = #{fingerprint} </if>",
            "</script>"
    })
    long countPage(@Param("linkId") Long linkId,
                   @Param("result") String result,
                   @Param("fingerprint") String fingerprint);
}
