package com.winsalty.quickstart.system.dict.entity;

import lombok.Data;

/**
 * 字典类型实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DictTypeEntity {
    /** 字典类型主键ID。 */
    private Long id;
    /** 字典编码。 */
    private String dictCode;
    /** 字典名称。 */
    private String dictName;
    /** 字典类型标识。 */
    private String dictType;
    /** 字典状态。 */
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
