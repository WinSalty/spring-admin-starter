package com.winsalty.quickstart.credential.dto;

import lombok.Data;

/**
 * 凭证提取链接分页查询请求。
 * 支持按链接、分类、批次、状态、创建和访问时间筛选链接。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Data
public class CredentialExtractLinkListRequest {

    /** 关键字，匹配链接编号、批次号或批次名称。 */
    private String keyword;
    /** 分类ID。 */
    private Long categoryId;
    /** 批次ID。 */
    private Long batchId;
    /** 状态。 */
    private String status;
    /** 创建人ID。 */
    private Long createdBy;
    /** 过期开始时间。 */
    private String expireFrom;
    /** 过期结束时间。 */
    private String expireTo;
    /** 最近访问开始时间。 */
    private String lastAccessFrom;
    /** 最近访问结束时间。 */
    private String lastAccessTo;
    /** 页码。 */
    private Integer pageNo;
    /** 每页条数。 */
    private Integer pageSize;
}
