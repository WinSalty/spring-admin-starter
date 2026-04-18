package com.winsalty.quickstart.system.entity;

/**
 * 系统配置实体对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecordCode() {
        return recordCode;
    }

    public void setRecordCode(String recordCode) {
        this.recordCode = recordCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
