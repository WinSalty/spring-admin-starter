package com.winsalty.quickstart.system.dict.vo;

import lombok.Data;

/**
 * 字典项展示对象。
 * 用于返回字典类型、标签、值、排序号、状态和时间字段。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
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
