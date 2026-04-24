package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * CDK 批次数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface CdkBatchMapper {

    String BATCH_SELECT = "SELECT id, batch_no AS batchNo, batch_name AS batchName, benefit_type AS benefitType, benefit_config AS benefitConfig, "
            + "total_count AS totalCount, generated_count AS generatedCount, redeemed_count AS redeemedCount, "
            + "DATE_FORMAT(valid_from, '%Y-%m-%d %H:%i:%s') AS validFrom, DATE_FORMAT(valid_to, '%Y-%m-%d %H:%i:%s') AS validTo, "
            + "status, risk_level AS riskLevel, created_by AS createdBy, approved_by AS approvedBy, "
            + "DATE_FORMAT(approved_at, '%Y-%m-%d %H:%i:%s') AS approvedAt, export_count AS exportCount, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM cdk_batch ";

    @Insert("INSERT INTO cdk_batch(batch_no, batch_name, benefit_type, benefit_config, total_count, generated_count, redeemed_count, valid_from, valid_to, status, risk_level, created_by, export_count) VALUES(#{batchNo}, #{batchName}, #{benefitType}, #{benefitConfig}, #{totalCount}, #{generatedCount}, #{redeemedCount}, STR_TO_DATE(#{validFrom}, '%Y-%m-%d %H:%i:%s'), STR_TO_DATE(#{validTo}, '%Y-%m-%d %H:%i:%s'), #{status}, #{riskLevel}, #{createdBy}, #{exportCount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkBatchEntity entity);

    @Select(BATCH_SELECT + "WHERE id = #{id} LIMIT 1")
    CdkBatchEntity findById(@Param("id") Long id);

    @Select(BATCH_SELECT + "WHERE id = #{id} LIMIT 1 FOR UPDATE")
    CdkBatchEntity findByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE cdk_batch SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE cdk_batch SET status = 'active', generated_count = #{generatedCount}, approved_by = #{approvedBy}, approved_at = NOW() WHERE id = #{id}")
    int markApproved(@Param("id") Long id,
                     @Param("generatedCount") Integer generatedCount,
                     @Param("approvedBy") String approvedBy);

    @Update("UPDATE cdk_batch SET redeemed_count = redeemed_count + 1 WHERE id = #{id}")
    int incrementRedeemed(@Param("id") Long id);

    @Update("UPDATE cdk_batch SET export_count = export_count + 1 WHERE id = #{id}")
    int incrementExport(@Param("id") Long id);

    @Select({
            "<script>",
            BATCH_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CdkBatchEntity> findPage(@Param("keyword") String keyword,
                                  @Param("status") String status,
                                  @Param("benefitType") String benefitType,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM cdk_batch WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(batch_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(batch_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("benefitType") String benefitType);
}
