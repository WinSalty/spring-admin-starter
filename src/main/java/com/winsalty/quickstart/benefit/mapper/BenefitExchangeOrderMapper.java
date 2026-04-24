package com.winsalty.quickstart.benefit.mapper;

import com.winsalty.quickstart.benefit.entity.BenefitExchangeOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 权益兑换订单数据访问接口。
 * 提供订单创建、幂等查询、结果更新和分页审计能力。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface BenefitExchangeOrderMapper {

    String ORDER_SELECT = "SELECT id, order_no AS orderNo, user_id AS userId, product_id AS productId, product_no AS productNo, "
            + "benefit_type AS benefitType, benefit_code AS benefitCode, cost_points AS costPoints, freeze_no AS freezeNo, "
            + "status, failure_message AS failureMessage, idempotency_key AS idempotencyKey, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM benefit_exchange_order ";

    @Insert("INSERT INTO benefit_exchange_order(order_no, user_id, product_id, product_no, benefit_type, benefit_code, cost_points, freeze_no, status, failure_message, idempotency_key) VALUES(#{orderNo}, #{userId}, #{productId}, #{productNo}, #{benefitType}, #{benefitCode}, #{costPoints}, #{freezeNo}, #{status}, #{failureMessage}, #{idempotencyKey})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BenefitExchangeOrderEntity entity);

    @Select(ORDER_SELECT + "WHERE user_id = #{userId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    BenefitExchangeOrderEntity findByUserIdAndIdempotencyKey(@Param("userId") Long userId,
                                                             @Param("idempotencyKey") String idempotencyKey);

    @Update("UPDATE benefit_exchange_order SET status = #{status}, failure_message = #{failureMessage} WHERE order_no = #{orderNo}")
    int updateStatus(@Param("orderNo") String orderNo,
                     @Param("status") String status,
                     @Param("failureMessage") String failureMessage);

    @Select({
            "<script>",
            ORDER_SELECT,
            "WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<BenefitExchangeOrderEntity> findPage(@Param("userId") Long userId,
                                              @Param("benefitType") String benefitType,
                                              @Param("status") String status,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM benefit_exchange_order WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='benefitType != null and benefitType != \"\"'>AND benefit_type = #{benefitType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("userId") Long userId,
                   @Param("benefitType") String benefitType,
                   @Param("status") String status);
}
