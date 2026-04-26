package com.winsalty.quickstart.cdk.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkBatchListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeStatusRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.service.CdkService;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkCodeVo;
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
 * 提供批次生成、CDK 在线查看、状态管理、批次作废和兑换记录查询接口。
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
        return ApiResponse.success("生成成功", cdkService.createBatch(request));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_void", name = "作废CDK批次")
    @PostMapping("/batches/{id}/void")
    public ApiResponse<CdkBatchVo> voidBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("已作废", cdkService.voidBatch(id));
    }

    @GetMapping("/codes")
    public ApiResponse<PageResponse<CdkCodeVo>> codes(@Validated CdkCodeListRequest request) {
        return ApiResponse.success("获取成功", cdkService.listCodes(request));
    }

    @AuditLog(logType = "operation", code = "cdk_code_status", name = "变更CDK状态")
    @PostMapping("/codes/{id}/status")
    public ApiResponse<CdkCodeVo> updateCodeStatus(@PathVariable("id") Long id,
                                                   @Valid @RequestBody CdkCodeStatusRequest request) {
        return ApiResponse.success("状态已更新", cdkService.updateCodeStatus(id, request));
    }

    @GetMapping("/redeem-records")
    public ApiResponse<PageResponse<CdkRedeemRecordVo>> redeemRecords(@Validated CdkRedeemRecordListRequest request) {
        return ApiResponse.success("获取成功", cdkService.listRedeemRecords(request));
    }
}
