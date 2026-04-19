package com.winsalty.quickstart.department.entity;

import lombok.Data;

/**
 * 部门实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DepartmentEntity {

    /** 部门主键ID。 */
    private Long id;
    /** 部门名称。 */
    private String name;
    /** 部门编码。 */
    private String code;
    /** 父级部门ID。 */
    private Long parentId;
    /** 排序号。 */
    private Integer sortOrder;
    /** 部门负责人。 */
    private String leader;
    /** 联系电话。 */
    private String phone;
    /** 联系邮箱。 */
    private String email;
    /** 部门状态。 */
    private String status;
    /** 逻辑删除标记。 */
    private Integer deleted;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
