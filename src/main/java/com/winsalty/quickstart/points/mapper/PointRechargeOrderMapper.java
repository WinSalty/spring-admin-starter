package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 积分充值单数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointRechargeOrderMapper {

    String RECHARGE_SELECT = "SELECT id, recharge_no AS rechargeNo, user_id AS userId, channel, amount, status, external_no AS externalNo, "
            + "idempotency_key AS idempotencyKey, request_snapshot AS requestSnapshot, result_snapshot AS resultSnapshot, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM point_recharge_order ";

    @Insert("INSERT INTO point_recharge_order(recharge_no, user_id, channel, amount, status, external_no, idempotency_key, request_snapshot, result_snapshot) VALUES(#{rechargeNo}, #{userId}, #{channel}, #{amount}, #{status}, #{externalNo}, #{idempotencyKey}, #{requestSnapshot}, #{resultSnapshot})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointRechargeOrderEntity entity);

    @Update("UPDATE point_recharge_order SET status = #{status}, result_snapshot = #{resultSnapshot} WHERE recharge_no = #{rechargeNo}")
    int updateStatus(@Param("rechargeNo") String rechargeNo,
                     @Param("status") String status,
                     @Param("resultSnapshot") String resultSnapshot);

    @Select(RECHARGE_SELECT + "WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<PointRechargeOrderEntity> findUserPage(@Param("userId") Long userId,
                                                @Param("offset") int offset,
                                                @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(1) FROM point_recharge_order WHERE user_id = #{userId}")
    long countUserPage(@Param("userId") Long userId);
}
