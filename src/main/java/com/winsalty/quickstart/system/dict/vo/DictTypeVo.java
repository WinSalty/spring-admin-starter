package com.winsalty.quickstart.system.dict.vo;

import lombok.Data;

@Data
public class DictTypeVo {
    private String id;
    private String dictName;
    private String dictType;
    private String status;
    private String remark;
    private Long itemCount;
    private String createdAt;
    private String updatedAt;
}
