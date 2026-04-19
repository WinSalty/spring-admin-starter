package com.winsalty.quickstart.system.dict.entity;

import lombok.Data;

/**
 * 字典数据实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Data
public class DictDataEntity {
    /** 字典数据主键ID。 */
    private Long id;
    /** 数据编码。 */
    private String dataCode;
    /** 字典类型ID。 */
    private Long dictTypeId;
    /** 字典类型标识。 */
    private String dictType;
    /** 显示标签。 */
    private String label;
    /** 字典值。 */
    private String value;
    /** 排序号。 */
    private Integer sortNo;
    /** 状态。 */
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
