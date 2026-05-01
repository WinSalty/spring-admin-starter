package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 凭证批次数据访问接口。
 * 提供批次分页、详情、数量统计更新和状态变更能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialBatchMapper {

    String BATCH_SELECT = "SELECT b.id, b.batch_no AS batchNo, b.batch_name AS batchName, b.category_id AS categoryId, "
            + "c.category_code AS categoryCode, c.category_name AS categoryName, b.fulfillment_type AS fulfillmentType, "
            + "b.generation_mode AS generationMode, b.payload_config AS payloadConfig, b.total_count AS totalCount, "
            + "b.available_count AS availableCount, b.consumed_count AS consumedCount, b.linked_count AS linkedCount, "
            + "DATE_FORMAT(b.valid_from, '%Y-%m-%d %H:%i:%s') AS validFrom, DATE_FORMAT(b.valid_to, '%Y-%m-%d %H:%i:%s') AS validTo, "
            + "b.status, b.created_by AS createdBy, DATE_FORMAT(b.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(b.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM credential_batch b "
            + "LEFT JOIN credential_category c ON c.id = b.category_id ";

    @Select({
            "<script>",
            BATCH_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='categoryId != null'>AND b.category_id = #{categoryId} </if>",
            "<if test='fulfillmentType != null and fulfillmentType != \"\"'>AND b.fulfillment_type = #{fulfillmentType} </if>",
            "<if test='generationMode != null and generationMode != \"\"'>AND b.generation_mode = #{generationMode} </if>",
            "<if test='status != null and status != \"\"'>AND b.status = #{status} </if>",
            "ORDER BY b.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CredentialBatchEntity> findPage(@Param("keyword") String keyword,
                                         @Param("categoryId") Long categoryId,
                                         @Param("fulfillmentType") String fulfillmentType,
                                         @Param("generationMode") String generationMode,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_batch b WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(b.batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(b.batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='categoryId != null'>AND b.category_id = #{categoryId} </if>",
            "<if test='fulfillmentType != null and fulfillmentType != \"\"'>AND b.fulfillment_type = #{fulfillmentType} </if>",
            "<if test='generationMode != null and generationMode != \"\"'>AND b.generation_mode = #{generationMode} </if>",
            "<if test='status != null and status != \"\"'>AND b.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("categoryId") Long categoryId,
                   @Param("fulfillmentType") String fulfillmentType,
                   @Param("generationMode") String generationMode,
                   @Param("status") String status);

    @Select(BATCH_SELECT + "WHERE b.id = #{id} LIMIT 1")
    CredentialBatchEntity findById(@Param("id") Long id);

    @Select(BATCH_SELECT + "WHERE b.id = #{id} LIMIT 1 FOR UPDATE")
    CredentialBatchEntity findByIdForUpdate(@Param("id") Long id);

    @Insert("INSERT INTO credential_batch(batch_no, batch_name, category_id, fulfillment_type, generation_mode, payload_config, total_count, available_count, consumed_count, linked_count, valid_from, valid_to, status, created_by) "
            + "VALUES(#{batchNo}, #{batchName}, #{categoryId}, #{fulfillmentType}, #{generationMode}, #{payloadConfig}, #{totalCount}, #{availableCount}, #{consumedCount}, #{linkedCount}, STR_TO_DATE(#{validFrom}, '%Y-%m-%d %H:%i:%s'), STR_TO_DATE(#{validTo}, '%Y-%m-%d %H:%i:%s'), #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialBatchEntity entity);

    @Update("UPDATE credential_batch SET status = 'disabled' WHERE id = #{id} AND status = 'active'")
    int disable(@Param("id") Long id);

    @Update("UPDATE credential_batch SET linked_count = linked_count + #{linkedCount}, available_count = GREATEST(available_count - #{availableDelta}, 0) WHERE id = #{id}")
    int increaseLinked(@Param("id") Long id, @Param("linkedCount") int linkedCount, @Param("availableDelta") int availableDelta);

    @Update("UPDATE credential_batch SET consumed_count = consumed_count + #{consumedCount}, available_count = GREATEST(available_count - #{availableDelta}, 0) WHERE id = #{id}")
    int increaseConsumed(@Param("id") Long id, @Param("consumedCount") int consumedCount, @Param("availableDelta") int availableDelta);
}
