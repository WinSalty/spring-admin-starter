package com.winsalty.quickstart.param.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ParamSaveRequest {
    private String id;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 64, message = "参数名称长度不能超过 64")
    private String configName;

    @NotBlank(message = "参数键不能为空")
    @Size(max = 128, message = "参数键长度不能超过 128")
    private String configKey;

    @NotBlank(message = "参数值不能为空")
    @Size(max = 500, message = "参数值长度不能超过 500")
    private String configValue;

    @NotBlank(message = "值类型不能为空")
    @Pattern(regexp = "string|boolean|number", message = "值类型不合法")
    private String valueType;

    @NotBlank(message = "参数类型不能为空")
    @Size(max = 32, message = "参数类型长度不能超过 32")
    private String configType;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
