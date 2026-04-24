package com.winsalty.quickstart.points.dto;

import lombok.Data;

/**
 * 当前用户积分流水查询请求。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class PointLedgerListRequest {

    /** 流水方向。 */
    private String direction;
    /** 业务类型。 */
    private String bizType;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
