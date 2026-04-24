package com.winsalty.quickstart.cdk.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkBatchListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.service.CdkService;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkExportVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemRecordVo;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端 CDK 控制器。
 * 提供批次创建、审批生成、一次性导出、暂停作废和兑换记录查询接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/cdk")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCdkController {

    private final CdkService cdkService;

    public AdminCdkController(CdkService cdkService) {
        this.cdkService = cdkService;
    }

    @GetMapping("/batches")
    public ApiResponse<PageResponse<CdkBatchVo>> batches(@Validated CdkBatchListRequest request) {
        return ApiResponse.success("获取成功", cdkService.listBatches(request));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_create", name = "创建CDK批次")
    @PostMapping("/batches")
    public ApiResponse<CdkBatchVo> createBatch(@Valid @RequestBody CdkBatchCreateRequest request) {
        return ApiResponse.success("创建成功", cdkService.createBatch(request));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_submit", name = "提交CDK批次审批")
    @PostMapping("/batches/{id}/submit")
    public ApiResponse<CdkBatchVo> submitBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("已提交审批", cdkService.submitBatch(id));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_approve", name = "审批并生成CDK批次")
    @PostMapping("/batches/{id}/approve")
    public ApiResponse<CdkBatchVo> approveBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("审批通过并已生成", cdkService.approveBatch(id));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_pause", name = "暂停CDK批次")
    @PostMapping("/batches/{id}/pause")
    public ApiResponse<CdkBatchVo> pauseBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("已暂停", cdkService.pauseBatch(id));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_void", name = "作废CDK批次")
    @PostMapping("/batches/{id}/void")
    public ApiResponse<CdkBatchVo> voidBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("已作废", cdkService.voidBatch(id));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_export", name = "导出CDK批次", recordResponse = false)
    @PostMapping("/batches/{id}/export")
    public ApiResponse<CdkExportVo> exportBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("导出成功", cdkService.exportBatch(id));
    }

    @GetMapping("/redeem-records")
    public ApiResponse<PageResponse<CdkRedeemRecordVo>> redeemRecords(@Validated CdkRedeemRecordListRequest request) {
        return ApiResponse.success("获取成功", cdkService.listRedeemRecords(request));
    }
}
