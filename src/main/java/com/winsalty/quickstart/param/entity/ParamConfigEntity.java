package com.winsalty.quickstart.param.entity;

import lombok.Data;

/**
 * 参数配置实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class ParamConfigEntity {
    /** 参数主键ID。 */
    private Long id;
    /** 参数记录编码。 */
    private String configCode;
    /** 参数名称。 */
    private String configName;
    /** 参数键。 */
    private String configKey;
    /** 参数值。 */
    private String configValue;
    /** 值类型。 */
    private String valueType;
    /** 参数分类。 */
    private String configType;
    /** 参数状态。 */
    private String status;
    /** 备注信息。 */
    private String remark;
    /** 逻辑删除标记。 */
    private Integer deleted;
    /** 创建时间。 */
    private String createdAt;
    /** 更新时间。 */
    private String updatedAt;
}
