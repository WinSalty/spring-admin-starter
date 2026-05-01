package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 凭证提取链接数据访问接口。
 * 提供链接管理页所需的分页、详情、停用和延期操作。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialExtractLinkMapper {

    String LINK_SELECT = "SELECT l.id, l.link_no AS linkNo, l.category_id AS categoryId, c.category_name AS categoryName, "
            + "l.batch_id AS batchId, b.batch_no AS batchNo, b.batch_name AS batchName, l.token_hash AS tokenHash, "
            + "l.encrypted_token AS encryptedToken, l.token_key_id AS tokenKeyId, l.item_count AS itemCount, "
            + "l.max_access_count AS maxAccessCount, l.accessed_count AS accessedCount, "
            + "DATE_FORMAT(l.expire_at, '%Y-%m-%d %H:%i:%s') AS expireAt, l.status, l.created_by AS createdBy, "
            + "l.disabled_by AS disabledBy, DATE_FORMAT(l.disabled_at, '%Y-%m-%d %H:%i:%s') AS disabledAt, "
            + "DATE_FORMAT(l.last_accessed_at, '%Y-%m-%d %H:%i:%s') AS lastAccessedAt, l.remark, "
            + "DATE_FORMAT(l.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(l.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM credential_extract_link l "
            + "LEFT JOIN credential_category c ON c.id = l.category_id "
            + "LEFT JOIN credential_batch b ON b.id = l.batch_id ";

    @Select({
            "<script>",
            LINK_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(l.link_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='categoryId != null'>AND l.category_id = #{categoryId} </if>",
            "<if test='batchId != null'>AND l.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND l.status = #{status} </if>",
            "<if test='createdBy != null'>AND l.created_by = #{createdBy} </if>",
            "<if test='expireFrom != null and expireFrom != \"\"'>AND l.expire_at &gt;= STR_TO_DATE(#{expireFrom}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='expireTo != null and expireTo != \"\"'>AND l.expire_at &lt;= STR_TO_DATE(#{expireTo}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='lastAccessFrom != null and lastAccessFrom != \"\"'>AND l.last_accessed_at &gt;= STR_TO_DATE(#{lastAccessFrom}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='lastAccessTo != null and lastAccessTo != \"\"'>AND l.last_accessed_at &lt;= STR_TO_DATE(#{lastAccessTo}, '%Y-%m-%d %H:%i:%s') </if>",
            "ORDER BY l.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CredentialExtractLinkEntity> findPage(@Param("keyword") String keyword,
                                               @Param("categoryId") Long categoryId,
                                               @Param("batchId") Long batchId,
                                               @Param("status") String status,
                                               @Param("createdBy") Long createdBy,
                                               @Param("expireFrom") String expireFrom,
                                               @Param("expireTo") String expireTo,
                                               @Param("lastAccessFrom") String lastAccessFrom,
                                               @Param("lastAccessTo") String lastAccessTo,
                                               @Param("offset") int offset,
                                               @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_extract_link l LEFT JOIN credential_batch b ON b.id = l.batch_id WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(l.link_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='categoryId != null'>AND l.category_id = #{categoryId} </if>",
            "<if test='batchId != null'>AND l.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND l.status = #{status} </if>",
            "<if test='createdBy != null'>AND l.created_by = #{createdBy} </if>",
            "<if test='expireFrom != null and expireFrom != \"\"'>AND l.expire_at &gt;= STR_TO_DATE(#{expireFrom}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='expireTo != null and expireTo != \"\"'>AND l.expire_at &lt;= STR_TO_DATE(#{expireTo}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='lastAccessFrom != null and lastAccessFrom != \"\"'>AND l.last_accessed_at &gt;= STR_TO_DATE(#{lastAccessFrom}, '%Y-%m-%d %H:%i:%s') </if>",
            "<if test='lastAccessTo != null and lastAccessTo != \"\"'>AND l.last_accessed_at &lt;= STR_TO_DATE(#{lastAccessTo}, '%Y-%m-%d %H:%i:%s') </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("categoryId") Long categoryId,
                   @Param("batchId") Long batchId,
                   @Param("status") String status,
                   @Param("createdBy") Long createdBy,
                   @Param("expireFrom") String expireFrom,
                   @Param("expireTo") String expireTo,
                   @Param("lastAccessFrom") String lastAccessFrom,
                   @Param("lastAccessTo") String lastAccessTo);

    @Select(LINK_SELECT + "WHERE l.id = #{id} LIMIT 1")
    CredentialExtractLinkEntity findById(@Param("id") Long id);

    @Select(LINK_SELECT + "WHERE l.id = #{id} LIMIT 1 FOR UPDATE")
    CredentialExtractLinkEntity findByIdForUpdate(@Param("id") Long id);

    @Select(LINK_SELECT + "WHERE l.token_hash = #{tokenHash} LIMIT 1 FOR UPDATE")
    CredentialExtractLinkEntity findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Insert("INSERT INTO credential_extract_link(link_no, category_id, batch_id, token_hash, encrypted_token, token_key_id, item_count, max_access_count, accessed_count, expire_at, status, created_by, remark) "
            + "VALUES(#{linkNo}, #{categoryId}, #{batchId}, #{tokenHash}, #{encryptedToken}, #{tokenKeyId}, #{itemCount}, #{maxAccessCount}, #{accessedCount}, STR_TO_DATE(#{expireAt}, '%Y-%m-%d %H:%i:%s'), #{status}, #{createdBy}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialExtractLinkEntity entity);

    @Update("UPDATE credential_extract_link SET status = 'disabled', disabled_by = #{disabledBy}, disabled_at = NOW(), remark = #{remark} WHERE id = #{id} AND status = 'active'")
    int disable(@Param("id") Long id, @Param("disabledBy") Long disabledBy, @Param("remark") String remark);

    @Update("UPDATE credential_extract_link SET expire_at = STR_TO_DATE(#{expireAt}, '%Y-%m-%d %H:%i:%s') WHERE id = #{id} AND status = 'active'")
    int extend(@Param("id") Long id, @Param("expireAt") String expireAt);

    @Update("UPDATE credential_extract_link SET accessed_count = accessed_count + 1, last_accessed_at = NOW(), status = CASE WHEN accessed_count + 1 >= max_access_count THEN 'exhausted' ELSE status END WHERE id = #{id} AND status = 'active' AND accessed_count < max_access_count")
    int increaseAccess(@Param("id") Long id);
}
