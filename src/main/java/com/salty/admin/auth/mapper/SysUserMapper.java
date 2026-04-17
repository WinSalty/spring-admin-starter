package com.salty.admin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.salty.admin.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE deleted = 0 AND (username = #{account} OR email = #{account}) LIMIT 1")
    SysUser selectByUsernameOrEmail(@Param("account") String account);
}
