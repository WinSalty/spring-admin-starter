package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointAdjustmentOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 积分人工调整单数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointAdjustmentOrderMapper {

    String ADJUST_SELECT = "SELECT id, adjust_no AS adjustNo, user_id AS userId, direction, amount, status, reason, ticket_no AS ticketNo, "
            + "idempotency_key AS idempotencyKey, applicant, approver, DATE_FORMAT(approved_at, '%Y-%m-%d %H:%i:%s') AS approvedAt, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM point_adjustment_order ";

    @Insert("INSERT INTO point_adjustment_order(adjust_no, user_id, direction, amount, status, reason, ticket_no, idempotency_key, applicant) VALUES(#{adjustNo}, #{userId}, #{direction}, #{amount}, #{status}, #{reason}, #{ticketNo}, #{idempotencyKey}, #{applicant})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointAdjustmentOrderEntity entity);

    @Select(ADJUST_SELECT + "WHERE id = #{id} LIMIT 1")
    PointAdjustmentOrderEntity findById(@Param("id") Long id);

    @Update("UPDATE point_adjustment_order SET status = #{status}, approver = #{approver}, approved_at = NOW() WHERE id = #{id} AND status = 'pending'")
    int updateApproval(@Param("id") Long id,
                       @Param("status") String status,
                       @Param("approver") String approver);
}
