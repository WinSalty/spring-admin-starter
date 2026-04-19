package com.winsalty.quickstart.query.entity;

import lombok.Data;

/**
 * 查询配置实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class QueryRecordEntity {

    /** 查询配置主键ID。 */
    private Long id;
    /** 查询记录编码。 */
    private String recordCode;
    /** 查询名称。 */
    private String name;
    /** 查询编码。 */
    private String code;
    /** 查询状态。 */
    private String status;
    /** 负责人。 */
    private String owner;
    /** 描述信息。 */
    private String description;
    /** 调用次数。 */
    private Long callCount;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
