package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 凭证明细数据访问接口。
 * 提供明细分页、去重查询、加锁读取和状态流转能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialItemMapper {

    String ITEM_SELECT = "SELECT i.id, i.batch_id AS batchId, i.category_id AS categoryId, i.item_no AS itemNo, "
            + "i.secret_hash AS secretHash, i.encrypted_secret AS encryptedSecret, i.secret_mask AS secretMask, i.checksum, "
            + "i.payload_snapshot AS payloadSnapshot, i.source_type AS sourceType, i.source_line_no AS sourceLineNo, i.status, "
            + "i.consumed_user_id AS consumedUserId, DATE_FORMAT(i.consumed_at, '%Y-%m-%d %H:%i:%s') AS consumedAt, "
            + "i.consume_biz_no AS consumeBizNo, i.version, DATE_FORMAT(i.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(i.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM credential_item i ";

    @Select({
            "<script>",
            ITEM_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(i.item_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(i.secret_mask) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='batchId != null'>AND i.batch_id = #{batchId} </if>",
            "<if test='categoryId != null'>AND i.category_id = #{categoryId} </if>",
            "<if test='sourceType != null and sourceType != \"\"'>AND i.source_type = #{sourceType} </if>",
            "<if test='status != null and status != \"\"'>AND i.status = #{status} </if>",
            "ORDER BY i.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CredentialItemEntity> findPage(@Param("keyword") String keyword,
                                        @Param("batchId") Long batchId,
                                        @Param("categoryId") Long categoryId,
                                        @Param("sourceType") String sourceType,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_item i WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(i.item_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(i.secret_mask) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='batchId != null'>AND i.batch_id = #{batchId} </if>",
            "<if test='categoryId != null'>AND i.category_id = #{categoryId} </if>",
            "<if test='sourceType != null and sourceType != \"\"'>AND i.source_type = #{sourceType} </if>",
            "<if test='status != null and status != \"\"'>AND i.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("batchId") Long batchId,
                   @Param("categoryId") Long categoryId,
                   @Param("sourceType") String sourceType,
                   @Param("status") String status);

    @Select(ITEM_SELECT + "WHERE i.id = #{id} LIMIT 1")
    CredentialItemEntity findById(@Param("id") Long id);

    @Select(ITEM_SELECT + "WHERE i.id = #{id} LIMIT 1 FOR UPDATE")
    CredentialItemEntity findByIdForUpdate(@Param("id") Long id);

    @Select(ITEM_SELECT + "WHERE i.secret_hash = #{secretHash} LIMIT 1")
    CredentialItemEntity findBySecretHash(@Param("secretHash") String secretHash);

    @Select("SELECT COUNT(1) FROM credential_item WHERE secret_hash = #{secretHash}")
    long countBySecretHash(@Param("secretHash") String secretHash);

    @Select(ITEM_SELECT + "WHERE i.batch_id = #{batchId} AND i.status = 'active' ORDER BY i.id ASC LIMIT #{limit} FOR UPDATE")
    List<CredentialItemEntity> findActiveByBatchForUpdate(@Param("batchId") Long batchId, @Param("limit") int limit);

    @Select(ITEM_SELECT + "WHERE i.batch_id = #{batchId} ORDER BY i.id ASC")
    List<CredentialItemEntity> findByBatchId(@Param("batchId") Long batchId);

    @Select(ITEM_SELECT + "WHERE i.id IN (${ids}) ORDER BY i.id ASC")
    List<CredentialItemEntity> findByUnsafeIds(@Param("ids") String ids);

    @Insert("INSERT INTO credential_item(batch_id, category_id, item_no, secret_hash, encrypted_secret, secret_mask, checksum, payload_snapshot, source_type, source_line_no, status) "
            + "VALUES(#{batchId}, #{categoryId}, #{itemNo}, #{secretHash}, #{encryptedSecret}, #{secretMask}, #{checksum}, #{payloadSnapshot}, #{sourceType}, #{sourceLineNo}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialItemEntity entity);

    @Update("UPDATE credential_item SET status = 'linked', version = version + 1 WHERE id = #{id} AND status = 'active'")
    int markLinked(@Param("id") Long id);

    @Update("UPDATE credential_item SET status = 'extracted', consumed_at = NOW(), consume_biz_no = #{bizNo}, version = version + 1 WHERE id = #{id} AND status = 'linked'")
    int markExtracted(@Param("id") Long id, @Param("bizNo") String bizNo);

    @Update("UPDATE credential_item SET status = 'redeemed', consumed_user_id = #{userId}, consumed_at = NOW(), consume_biz_no = #{bizNo}, version = version + 1 WHERE id = #{id} AND status = 'active'")
    int markRedeemed(@Param("id") Long id, @Param("userId") Long userId, @Param("bizNo") String bizNo);

    @Update("UPDATE credential_item SET status = 'disabled', version = version + 1 WHERE id = #{id} AND status IN ('active', 'linked')")
    int disable(@Param("id") Long id);

    @Update("UPDATE credential_item SET status = 'active', version = version + 1 WHERE id = #{id} AND status = 'disabled'")
    int enable(@Param("id") Long id);
}
