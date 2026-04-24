package com.winsalty.quickstart.points.dto;

import lombok.Data;

/**
 * 管理端积分账户查询请求。
 * 支持按用户关键字和账户状态筛选。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Data
public class AdminPointAccountListRequest {

    /** 用户名、昵称或用户ID关键字。 */
    private String keyword;
    /** 账户状态。 */
    private String status;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
