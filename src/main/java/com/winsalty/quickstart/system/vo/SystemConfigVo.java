package com.winsalty.quickstart.system.vo;

import lombok.Data;

/**
 * 系统配置响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class SystemConfigVo {

    private String id;
    private String name;
    private String code;
    private String type;
    private Object value;
    private String description;
    private String updatedAt;
}
