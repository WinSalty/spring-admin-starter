package com.winsalty.quickstart.system.dict.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class DictDataSaveRequest {
    private String id;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 64, message = "字典类型长度不能超过 64")
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 64, message = "字典标签长度不能超过 64")
    private String label;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 64, message = "字典值长度不能超过 64")
    private String value;

    @NotNull(message = "排序号不能为空")
    @Min(value = 0, message = "排序号不能小于 0")
    private Integer sortNo;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
