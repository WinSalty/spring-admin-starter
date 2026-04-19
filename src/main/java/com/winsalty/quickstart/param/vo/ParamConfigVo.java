package com.winsalty.quickstart.param.vo;

import lombok.Data;

@Data
public class ParamConfigVo {
    private String id;
    private String configName;
    private String configKey;
    private Object configValue;
    private String valueType;
    private String configType;
    private String status;
    private String remark;
    private String createdAt;
    private String updatedAt;
}
