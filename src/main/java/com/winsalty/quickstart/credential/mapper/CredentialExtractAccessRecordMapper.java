package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialExtractAccessRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 凭证提取访问记录数据访问接口。
 * 提供链接详情抽屉中的访问审计分页查询。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialExtractAccessRecordMapper {

    String ACCESS_SELECT = "SELECT id, access_no AS accessNo, link_id AS linkId, batch_id AS batchId, item_count AS itemCount, "
            + "success, failure_reason AS failureReason, client_ip AS clientIp, user_agent_hash AS userAgentHash, "
            + "browser_fingerprint AS browserFingerprint, device_snapshot AS deviceSnapshot, trace_id AS traceId, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt FROM credential_extract_access_record ";

    @Select({
            "<script>",
            ACCESS_SELECT,
            "WHERE link_id = #{linkId} ",
            "<if test='success != null'>AND success = #{success} </if>",
            "<if test='fingerprint != null and fingerprint != \"\"'>AND browser_fingerprint = #{fingerprint} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CredentialExtractAccessRecordEntity> findPage(@Param("linkId") Long linkId,
                                                       @Param("success") Integer success,
                                                       @Param("fingerprint") String fingerprint,
                                                       @Param("offset") int offset,
                                                       @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_extract_access_record WHERE link_id = #{linkId} ",
            "<if test='success != null'>AND success = #{success} </if>",
            "<if test='fingerprint != null and fingerprint != \"\"'>AND browser_fingerprint = #{fingerprint} </if>",
            "</script>"
    })
    long countPage(@Param("linkId") Long linkId,
                   @Param("success") Integer success,
                   @Param("fingerprint") String fingerprint);

    @Insert("INSERT INTO credential_extract_access_record(access_no, link_id, batch_id, item_count, success, failure_reason, client_ip, user_agent_hash, browser_fingerprint, device_snapshot, trace_id) "
            + "VALUES(#{accessNo}, #{linkId}, #{batchId}, #{itemCount}, #{success}, #{failureReason}, #{clientIp}, #{userAgentHash}, #{browserFingerprint}, #{deviceSnapshot}, #{traceId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialExtractAccessRecordEntity entity);
}
