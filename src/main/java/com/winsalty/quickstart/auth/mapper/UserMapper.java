package com.winsalty.quickstart.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winsalty.quickstart.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT u.id, u.record_code AS recordCode, u.username, u.email, u.password, u.nickname, u.avatar_url AS avatarUrl, u.country, u.province, u.city, u.street_address AS streetAddress, u.phone_prefix AS phonePrefix, u.phone_number AS phoneNumber, u.notify_account AS notifyAccount, u.notify_system AS notifySystem, u.notify_todo AS notifyTodo, u.status, u.owner, u.description, u.department_id AS departmentId, u.last_login_at AS lastLoginAt, u.deleted, u.created_at AS createdAt, u.updated_at AS updatedAt FROM sys_user u WHERE u.username = #{username} AND u.deleted = 0 LIMIT 1")
    UserEntity findByUsername(@Param("username") String username);

    @Select("SELECT u.id, u.record_code AS recordCode, u.username, u.email, u.password, u.nickname, u.avatar_url AS avatarUrl, u.country, u.province, u.city, u.street_address AS streetAddress, u.phone_prefix AS phonePrefix, u.phone_number AS phoneNumber, u.notify_account AS notifyAccount, u.notify_system AS notifySystem, u.notify_todo AS notifyTodo, u.status, u.owner, u.description, u.department_id AS departmentId, u.last_login_at AS lastLoginAt, u.deleted, u.created_at AS createdAt, u.updated_at AS updatedAt FROM sys_user u WHERE u.id = #{userId} AND u.deleted = 0 LIMIT 1")
    UserEntity findActiveById(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Update("UPDATE sys_user SET email = #{email}, nickname = #{nickname}, description = #{description}, avatar_url = #{avatarUrl}, country = #{country}, province = #{province}, city = #{city}, street_address = #{streetAddress}, phone_prefix = #{phonePrefix}, phone_number = #{phoneNumber} WHERE id = #{id} AND deleted = 0")
    int updateProfile(UserEntity user);

    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id} AND deleted = 0")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Update("UPDATE sys_user SET notify_account = #{notifyAccount}, notify_system = #{notifySystem}, notify_todo = #{notifyTodo} WHERE id = #{id} AND deleted = 0")
    int updateNotificationSettings(UserEntity user);
}
