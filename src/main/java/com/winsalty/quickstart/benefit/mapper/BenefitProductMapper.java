package com.winsalty.quickstart.benefit.mapper;

import com.winsalty.quickstart.benefit.entity.BenefitProductEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 权益商品数据访问接口。
 * 提供商品创建、库存扣减、状态变更和分页查询能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface BenefitProductMapper {

    String PRODUCT_SELECT = "SELECT id, product_no AS productNo, product_name AS productName, benefit_type AS benefitType, "
            + "benefit_code AS benefitCode, benefit_name AS benefitName, benefit_config AS benefitConfig, cost_points AS costPoints, "
            + "stock_total AS stockTotal, stock_used AS stockUsed, DATE_FORMAT(valid_from, '%Y-%m-%d %H:%i:%s') AS validFrom, "
            + "DATE_FORMAT(valid_to, '%Y-%m-%d %H:%i:%s') AS validTo, status, created_by AS createdBy, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM benefit_product ";

    @Insert("INSERT INTO benefit_product(product_no, product_name, benefit_type, benefit_code, benefit_name, benefit_config, cost_points, stock_total, stock_used, valid_from, valid_to, status, created_by) VALUES(#{productNo}, #{productName}, #{benefitType}, #{benefitCode}, #{benefitName}, #{benefitConfig}, #{costPoints}, #{stockTotal}, #{stockUsed}, STR_TO_DATE(#{validFrom}, '%Y-%m-%d %H:%i:%s'), STR_TO_DATE(#{validTo}, '%Y-%m-%d %H:%i:%s'), #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BenefitProductEntity entity);

    @Update("UPDATE benefit_product SET product_name = #{productName}, benefit_type = #{benefitType}, benefit_code = #{benefitCode}, benefit_name = #{benefitName}, benefit_config = #{benefitConfig}, cost_points = #{costPoints}, stock_total = #{stockTotal}, valid_from = STR_TO_DATE(#{validFrom}, '%Y-%m-%d %H:%i:%s'), valid_to = STR_TO_DATE(#{validTo}, '%Y-%m-%d %H:%i:%s'), status = #{status} WHERE id = #{id}")
    int update(BenefitProductEntity entity);

    @Select(PRODUCT_SELECT + "WHERE id = #{id} LIMIT 1")
    BenefitProductEntity findById(@Param("id") Long id);

    @Select(PRODUCT_SELECT + "WHERE id = #{id} LIMIT 1 FOR UPDATE")
    BenefitProductEntity findByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE benefit_product SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Update("UPDATE benefit_product SET stock_used = stock_used + 1 WHERE id = #{id} AND (stock_total = -1 OR stock_used < stock_total)")
    int incrementStockUsed(@Param("id") Long id);

    @Select({
            "<script>",
            PRODUCT_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(product_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(product_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(benefit_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<BenefitProductEntity> findPage(@Param("keyword") String keyword,
                                        @Param("benefitType") String benefitType,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM benefit_product WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>AND (LOWER(product_no) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(product_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(benefit_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')) </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword,
                   @Param("benefitType") String benefitType,
                   @Param("status") String status);
}
