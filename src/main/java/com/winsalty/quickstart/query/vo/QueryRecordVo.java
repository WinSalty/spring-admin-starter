package com.winsalty.quickstart.query.vo;

import lombok.Data;

/**
 * 查询配置响应对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class QueryRecordVo {

    private String id;
    private String name;
    private String code;
    private String status;
    private String owner;
    private String description;
    private Long callCount;
    private String createdAt;
    private String updatedAt;
}
