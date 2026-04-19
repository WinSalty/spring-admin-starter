package com.winsalty.quickstart.system.entity;

import lombok.Data;

/**
 * 系统配置实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemConfigEntity {

    /** 配置主键ID。 */
    private Long id;
    /** 配置记录编码。 */
    private String recordCode;
    /** 配置名称。 */
    private String name;
    /** 配置编码。 */
    private String code;
    /** 配置分类。 */
    private String configType;
    /** 值类型。 */
    private String valueType;
    /** 配置值。 */
    private String configValue;
    /** 配置描述。 */
    private String description;
    /** 更新时间。 */
    private String updatedAt;
}
