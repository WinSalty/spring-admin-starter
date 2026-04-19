package com.winsalty.quickstart.system.dict.vo;

import lombok.Data;

@Data
public class DictDataVo {
    private String id;
    private String dictType;
    private String label;
    private String value;
    private Integer sortNo;
    private String status;
    private String remark;
    private String createdAt;
    private String updatedAt;
}
