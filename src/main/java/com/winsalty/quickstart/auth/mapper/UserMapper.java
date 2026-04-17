package com.winsalty.quickstart.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.winsalty.quickstart.auth.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT u.id, u.username, u.email, u.password, u.nickname, u.status, u.deleted, u.created_at AS createdAt, u.updated_at AS updatedAt FROM sys_user u WHERE u.username = #{username} AND u.deleted = 0 LIMIT 1")
    UserEntity findByUsername(@Param("username") String username);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
