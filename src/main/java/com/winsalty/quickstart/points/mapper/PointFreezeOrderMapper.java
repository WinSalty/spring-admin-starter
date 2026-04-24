package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 积分冻结单数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointFreezeOrderMapper {

    String FREEZE_SELECT = "SELECT id, freeze_no AS freezeNo, user_id AS userId, amount, biz_type AS bizType, biz_no AS bizNo, status, "
            + "DATE_FORMAT(expire_at, '%Y-%m-%d %H:%i:%s') AS expireAt, idempotency_key AS idempotencyKey, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM point_freeze_order ";

    @Insert("INSERT INTO point_freeze_order(freeze_no, user_id, amount, biz_type, biz_no, status, expire_at, idempotency_key) VALUES(#{freezeNo}, #{userId}, #{amount}, #{bizType}, #{bizNo}, #{status}, STR_TO_DATE(#{expireAt}, '%Y-%m-%d %H:%i:%s'), #{idempotencyKey})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointFreezeOrderEntity entity);

    @Select(FREEZE_SELECT + "WHERE freeze_no = #{freezeNo} LIMIT 1")
    PointFreezeOrderEntity findByFreezeNo(@Param("freezeNo") String freezeNo);

    @Update("UPDATE point_freeze_order SET status = #{status} WHERE freeze_no = #{freezeNo}")
    int updateStatus(@Param("freezeNo") String freezeNo, @Param("status") String status);

    @Select(FREEZE_SELECT + "WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<PointFreezeOrderEntity> findUserPage(@Param("userId") Long userId,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(1) FROM point_freeze_order WHERE user_id = #{userId}")
    long countUserPage(@Param("userId") Long userId);
}
