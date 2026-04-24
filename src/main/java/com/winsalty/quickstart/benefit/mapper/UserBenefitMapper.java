package com.winsalty.quickstart.benefit.mapper;

import com.winsalty.quickstart.benefit.entity.UserBenefitEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户权益数据访问接口。
 * 维护用户已获得的权限、服务包等权益。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface UserBenefitMapper {

    String USER_BENEFIT_SELECT = "SELECT id, user_id AS userId, benefit_type AS benefitType, benefit_code AS benefitCode, "
            + "benefit_name AS benefitName, source_type AS sourceType, source_no AS sourceNo, status, "
            + "DATE_FORMAT(effective_at, '%Y-%m-%d %H:%i:%s') AS effectiveAt, DATE_FORMAT(expire_at, '%Y-%m-%d %H:%i:%s') AS expireAt, "
            + "config_snapshot AS configSnapshot, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, "
            + "DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM user_benefit ";

    @Insert("INSERT INTO user_benefit(user_id, benefit_type, benefit_code, benefit_name, source_type, source_no, status, effective_at, expire_at, config_snapshot) VALUES(#{userId}, #{benefitType}, #{benefitCode}, #{benefitName}, #{sourceType}, #{sourceNo}, #{status}, STR_TO_DATE(#{effectiveAt}, '%Y-%m-%d %H:%i:%s'), STR_TO_DATE(#{expireAt}, '%Y-%m-%d %H:%i:%s'), #{configSnapshot})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserBenefitEntity entity);

    @Select(USER_BENEFIT_SELECT + "WHERE user_id = #{userId} ORDER BY id DESC LIMIT #{offset}, #{pageSize}")
    List<UserBenefitEntity> findUserPage(@Param("userId") Long userId,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(1) FROM user_benefit WHERE user_id = #{userId}")
    long countUserPage(@Param("userId") Long userId);

    @Select("SELECT benefit_code FROM user_benefit WHERE user_id = #{userId} AND benefit_type = 'permission' AND status = 'active' AND effective_at <= NOW() AND (expire_at IS NULL OR expire_at > NOW())")
    List<String> findActivePermissionCodes(@Param("userId") Long userId);
}
