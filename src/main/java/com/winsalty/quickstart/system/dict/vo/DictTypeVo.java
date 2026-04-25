package com.winsalty.quickstart.system.dict.vo;

import lombok.Data;

/**
 * 字典类型展示对象。
 * 用于返回字典类型基础信息及其字典项数量。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
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
