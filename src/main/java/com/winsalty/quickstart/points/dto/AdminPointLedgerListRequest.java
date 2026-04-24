package com.winsalty.quickstart.points.dto;

import lombok.Data;

/**
 * 管理端积分流水审计查询请求。
 * 支持按用户、方向、业务类型和业务单号筛选。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class AdminPointLedgerListRequest {

    /** 用户ID。 */
    private Long userId;
    /** 流水方向。 */
    private String direction;
    /** 业务类型。 */
    private String bizType;
    /** 业务单号。 */
    private String bizNo;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
