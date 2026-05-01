package com.winsalty.quickstart.cdk.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkDisableRequest;
import com.winsalty.quickstart.cdk.service.CdkExtractService;
import com.winsalty.quickstart.cdk.vo.CdkBatchExtractLinkVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractAccessRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractLinkVo;
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
import java.util.List;

/**
 * 管理端 CDK 提取链接控制器。
 * 提供临时提取 URL 生成、停用、历史链接和访问审计查询能力。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/cdk")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCdkExtractController {

    private final CdkExtractService cdkExtractService;

    public AdminCdkExtractController(CdkExtractService cdkExtractService) {
        this.cdkExtractService = cdkExtractService;
    }

    @AuditLog(logType = "operation", code = "cdk_extract_link_create", name = "生成CDK提取链接")
    @PostMapping("/codes/{id}/extract-links")
    public ApiResponse<CdkExtractLinkVo> createLink(@PathVariable("id") Long id,
                                                    @Valid @RequestBody CdkExtractLinkCreateRequest request) {
        return ApiResponse.success("生成成功", cdkExtractService.createLink(id, request));
    }

    @AuditLog(logType = "operation", code = "cdk_batch_extract_link_create", name = "批量生成CDK提取链接")
    @PostMapping("/batches/{id}/extract-links")
    public ApiResponse<CdkBatchExtractLinkVo> createBatchLinks(@PathVariable("id") Long id,
                                                               @Valid @RequestBody CdkExtractLinkCreateRequest request) {
        return ApiResponse.success("生成成功", cdkExtractService.createBatchLinks(id, request));
    }

    @GetMapping("/codes/{id}/extract-links")
    public ApiResponse<List<CdkExtractLinkVo>> links(@PathVariable("id") Long id) {
        return ApiResponse.success("获取成功", cdkExtractService.listLinks(id));
    }

    @AuditLog(logType = "operation", code = "cdk_extract_link_disable", name = "停用CDK提取链接")
    @PostMapping("/extract-links/{id}/disable")
    public ApiResponse<CdkExtractLinkVo> disableLink(@PathVariable("id") Long id,
                                                     @Valid @RequestBody CdkExtractLinkDisableRequest request) {
        return ApiResponse.success("已停用", cdkExtractService.disableLink(id, request));
    }

    @GetMapping("/extract-links/{id}/access-records")
    public ApiResponse<PageResponse<CdkExtractAccessRecordVo>> accessRecords(@PathVariable("id") Long id,
                                                                             @Validated CdkExtractAccessRecordListRequest request) {
        return ApiResponse.success("获取成功", cdkExtractService.listAccessRecords(id, request));
    }
}
