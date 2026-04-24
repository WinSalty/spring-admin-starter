package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointAccountEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 积分账户数据访问接口。
 * 所有余额变更必须由积分服务调用，业务模块不得直接操作该 Mapper。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointAccountMapper {

    String ACCOUNT_SELECT = "SELECT a.id, a.user_id AS userId, u.username, u.nickname, "
            + "a.available_points AS availablePoints, a.frozen_points AS frozenPoints, "
            + "a.total_earned_points AS totalEarnedPoints, a.total_spent_points AS totalSpentPoints, "
            + "a.version, a.status, DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(a.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM point_account a LEFT JOIN sys_user u ON u.id = a.user_id ";

    @Select(ACCOUNT_SELECT + "WHERE a.user_id = #{userId} LIMIT 1")
    PointAccountEntity findByUserId(@Param("userId") Long userId);

    @Select(ACCOUNT_SELECT + "WHERE a.user_id = #{userId} LIMIT 1 FOR UPDATE")
    PointAccountEntity findByUserIdForUpdate(@Param("userId") Long userId);

    @Select(ACCOUNT_SELECT + "WHERE a.id = #{id} LIMIT 1")
    PointAccountEntity findById(@Param("id") Long id);

    @Insert("INSERT IGNORE INTO point_account(user_id, available_points, frozen_points, total_earned_points, total_spent_points, version, status) VALUES(#{userId}, 0, 0, 0, 0, 0, 'active')")
    int insertIgnore(@Param("userId") Long userId);

    @Update("UPDATE point_account SET available_points = #{availablePoints}, frozen_points = #{frozenPoints}, total_earned_points = #{totalEarnedPoints}, total_spent_points = #{totalSpentPoints}, version = version + 1 WHERE id = #{id} AND version = #{version}")
    int updateBalance(PointAccountEntity entity);

    @Select({
            "<script>",
            ACCOUNT_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (CAST(a.user_id AS CHAR) = #{keyword} OR LOWER(u.username) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(u.nickname, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND a.status = #{status} </if>",
            "ORDER BY a.updated_at DESC, a.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<PointAccountEntity> findPage(@Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM point_account a LEFT JOIN sys_user u ON u.id = a.user_id WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (CAST(a.user_id AS CHAR) = #{keyword} OR LOWER(u.username) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(u.nickname, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND a.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword, @Param("status") String status);

    @Select("SELECT COUNT(1) FROM point_account")
    long countAccounts();

    @Select("SELECT IFNULL(SUM(available_points), 0) FROM point_account")
    Long sumAvailable();

    @Select("SELECT IFNULL(SUM(frozen_points), 0) FROM point_account")
    Long sumFrozen();
}
