package com.winsalty.quickstart.system.dict.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class DictTypeSaveRequest {
    private String id;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 64, message = "字典名称长度不能超过 64")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 64, message = "字典类型长度不能超过 64")
    private String dictType;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
}
