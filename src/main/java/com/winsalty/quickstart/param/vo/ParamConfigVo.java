package com.winsalty.quickstart.param.vo;

import lombok.Data;

/**
 * 参数配置展示对象。
 * 用于管理端返回参数基础信息、类型化参数值和时间字段。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
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
