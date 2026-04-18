package com.winsalty.quickstart.system.dict.entity;

/**
 * 字典类型实体。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
