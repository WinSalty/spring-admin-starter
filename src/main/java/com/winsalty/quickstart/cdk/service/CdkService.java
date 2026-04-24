package com.winsalty.quickstart.cdk.service;

import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkBatchListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRequest;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkExportVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemResultVo;
import com.winsalty.quickstart.common.api.PageResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * CDK 服务接口。
 * 负责批次管理、生成导出、兑换和兑换记录审计。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface CdkService {

    PageResponse<CdkBatchVo> listBatches(CdkBatchListRequest request);

    CdkBatchVo createBatch(CdkBatchCreateRequest request);

    CdkBatchVo submitBatch(Long id);

    CdkBatchVo approveBatch(Long id);

    CdkBatchVo pauseBatch(Long id);

    CdkBatchVo voidBatch(Long id);

    CdkExportVo exportBatch(Long id);

    CdkRedeemResultVo redeem(CdkRedeemRequest request, HttpServletRequest servletRequest);

    PageResponse<CdkRedeemRecordVo> listRedeemRecords(CdkRedeemRecordListRequest request);
}
