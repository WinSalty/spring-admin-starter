package com.winsalty.quickstart.system.dict.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

@Data
public class DictDataListRequest {
    private String dictType;
    private String keyword;

    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    @Min(value = 1, message = "pageNo 不能小于 1")
    private Integer pageNo = 1;

    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = 200, message = "pageSize 不能大于 200")
    private Integer pageSize = 10;
}
