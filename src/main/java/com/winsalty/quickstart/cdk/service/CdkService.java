package com.winsalty.quickstart.cdk.service;

import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkBatchListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeStatusRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRequest;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkCodeVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemResultVo;
import com.winsalty.quickstart.common.api.PageResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * CDK 服务接口。
 * 负责批次生成、CDK 管理、兑换和兑换记录审计。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface CdkService {

    PageResponse<CdkBatchVo> listBatches(CdkBatchListRequest request);

    CdkBatchVo createBatch(CdkBatchCreateRequest request);

    CdkBatchVo voidBatch(Long id);

    PageResponse<CdkCodeVo> listCodes(CdkCodeListRequest request);

    CdkCodeVo updateCodeStatus(Long id, CdkCodeStatusRequest request);

    CdkRedeemResultVo redeem(CdkRedeemRequest request, HttpServletRequest servletRequest);

    PageResponse<CdkRedeemRecordVo> listRedeemRecords(CdkRedeemRecordListRequest request);
}
