package com.winsalty.quickstart.query.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 查询配置列表请求对象。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Data
public class QueryListRequest {

    private String keyword;
    private String status;

    @Min(value = 1, message = "pageNo 不能小于 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能大于 100")
    private Integer pageSize = 10;
}
