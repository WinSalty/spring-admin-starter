package com.winsalty.quickstart.auth.entity;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 用户实体。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@TableName("sys_user")
@Data
public class UserEntity {

    /** 用户主键ID。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户记录编码。 */
    private String recordCode;
    /** 登录用户名。 */
    private String username;
    /** 用户邮箱地址。 */
    private String email;
    /** 登录密码密文。 */
    private String password;
    /** 用户昵称。 */
    private String nickname;
    /** 用户状态。 */
    private String status;
    /** 归属负责人。 */
    private String owner;
    /** 用户描述信息。 */
    private String description;
    /** 所属部门ID。 */
    private Long departmentId;
    /** 最近登录时间。 */
    private LocalDateTime lastLoginAt;
    /** 逻辑删除标记。 */
    private Integer deleted;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 更新时间。 */
    private LocalDateTime updatedAt;
}
