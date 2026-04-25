package com.winsalty.quickstart.system.dict.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

/**
 * 字典类型分页查询请求。
 * 承载关键字、状态和分页参数。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Data
public class DictTypeListRequest {
    private String keyword;

    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Min(value = 1, message = "pageNo 不能小于 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 100, message = "pageSize 不能大于 100")
    private Integer pageSize = 10;
}
