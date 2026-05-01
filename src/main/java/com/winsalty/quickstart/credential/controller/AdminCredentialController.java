package com.winsalty.quickstart.credential.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.credential.dto.CredentialBatchListRequest;
import com.winsalty.quickstart.credential.dto.CredentialCategorySaveRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialGeneratedBatchCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportConfirmRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportPreviewRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportTaskListRequest;
import com.winsalty.quickstart.credential.dto.CredentialItemListRequest;
import com.winsalty.quickstart.credential.dto.CredentialRedeemRecordListRequest;
import com.winsalty.quickstart.credential.service.CredentialAdminService;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.vo.CredentialBatchVo;
import com.winsalty.quickstart.credential.vo.CredentialCategoryVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCreateResultVo;
import com.winsalty.quickstart.credential.vo.CredentialImportPreviewVo;
import com.winsalty.quickstart.credential.vo.CredentialImportTaskVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;
import com.winsalty.quickstart.credential.vo.CredentialRedeemRecordVo;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 管理端凭证中心控制器。
 * 提供分类、批次、明细、导入任务、兑换记录和生成提取链接接口。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/credentials")
public class AdminCredentialController {

    private final CredentialAdminService credentialAdminService;
    private final CredentialExtractLinkService credentialExtractLinkService;

    public AdminCredentialController(CredentialAdminService credentialAdminService,
                                     CredentialExtractLinkService credentialExtractLinkService) {
        this.credentialAdminService = credentialAdminService;
        this.credentialExtractLinkService = credentialExtractLinkService;
    }

    /** 查询凭证分类。 */
    @GetMapping("/categories")
    public ApiResponse<List<CredentialCategoryVo>> categories() {
        return ApiResponse.success("获取成功", credentialAdminService.listCategories());
    }

    /** 新增凭证分类。 */
    @AuditLog(logType = "operation", code = "credential_category_create", name = "新增凭证分类")
    @PostMapping("/categories")
    public ApiResponse<CredentialCategoryVo> createCategory(@Valid @RequestBody CredentialCategorySaveRequest request) {
        return ApiResponse.success("创建成功", credentialAdminService.createCategory(request));
    }

    /** 更新凭证分类。 */
    @AuditLog(logType = "operation", code = "credential_category_update", name = "更新凭证分类")
    @PutMapping("/categories/{id}")
    public ApiResponse<CredentialCategoryVo> updateCategory(@PathVariable("id") Long id,
                                                            @Valid @RequestBody CredentialCategorySaveRequest request) {
        return ApiResponse.success("更新成功", credentialAdminService.updateCategory(id, request));
    }

    /** 停用凭证分类。 */
    @AuditLog(logType = "operation", code = "credential_category_disable", name = "停用凭证分类")
    @PostMapping("/categories/{id}/disable")
    public ApiResponse<CredentialCategoryVo> disableCategory(@PathVariable("id") Long id) {
        return ApiResponse.success("已停用", credentialAdminService.disableCategory(id));
    }

    /** 分页查询凭证批次。 */
    @GetMapping("/batches")
    public ApiResponse<PageResponse<CredentialBatchVo>> batches(@Validated CredentialBatchListRequest request) {
        return ApiResponse.success("获取成功", credentialAdminService.listBatches(request));
    }

    /** 创建系统生成凭证批次。 */
    @AuditLog(logType = "operation", code = "credential_batch_create_generated", name = "创建系统生成凭证批次")
    @PostMapping("/batches/generated")
    public ApiResponse<CredentialBatchVo> createGeneratedBatch(@Valid @RequestBody CredentialGeneratedBatchCreateRequest request) {
        return ApiResponse.success("创建成功", credentialAdminService.createGeneratedBatch(request));
    }

    /** 文本卡密导入预览。 */
    @PostMapping("/batches/imported/preview")
    public ApiResponse<CredentialImportPreviewVo> previewImport(@Valid @RequestBody CredentialImportPreviewRequest request) {
        return ApiResponse.success("解析成功", credentialAdminService.previewImport(request));
    }

    /** 文本卡密确认导入。 */
    @AuditLog(logType = "operation", code = "credential_batch_import_confirm", name = "确认导入卡密批次")
    @PostMapping("/batches/imported/confirm")
    public ApiResponse<CredentialBatchVo> confirmImport(@Valid @RequestBody CredentialImportConfirmRequest request) {
        return ApiResponse.success("导入成功", credentialAdminService.confirmImport(request));
    }

    /** 查询凭证批次详情。 */
    @GetMapping("/batches/{id}")
    public ApiResponse<CredentialBatchVo> batchDetail(@PathVariable("id") Long id) {
        return ApiResponse.success("获取成功", credentialAdminService.getBatch(id));
    }

    /** 停用凭证批次。 */
    @AuditLog(logType = "operation", code = "credential_batch_disable", name = "停用凭证批次")
    @PostMapping("/batches/{id}/disable")
    public ApiResponse<CredentialBatchVo> disableBatch(@PathVariable("id") Long id) {
        return ApiResponse.success("已停用", credentialAdminService.disableBatch(id));
    }

    /** 按批次生成提取链接。 */
    @AuditLog(logType = "operation", code = "credential_extract_link_create_batch", name = "按批次生成提取链接")
    @PostMapping("/batches/{id}/extract-links")
    public ApiResponse<CredentialExtractLinkCreateResultVo> createBatchLinks(@PathVariable("id") Long id,
                                                                             @Valid @RequestBody CredentialExtractLinkCreateRequest request) {
        return ApiResponse.success("生成成功", credentialExtractLinkService.createBatchLinks(id, request));
    }

    /** 分页查询凭证明细。 */
    @GetMapping("/items")
    public ApiResponse<PageResponse<CredentialItemVo>> items(@Validated CredentialItemListRequest request) {
        return ApiResponse.success("获取成功", credentialAdminService.listItems(request));
    }

    /** 按明细生成提取链接。 */
    @AuditLog(logType = "operation", code = "credential_extract_link_create_item", name = "按凭证明细生成提取链接")
    @PostMapping("/items/{id}/extract-links")
    public ApiResponse<CredentialExtractLinkCreateResultVo> createItemLink(@PathVariable("id") Long id,
                                                                           @Valid @RequestBody CredentialExtractLinkCreateRequest request) {
        return ApiResponse.success("生成成功", credentialExtractLinkService.createItemLink(id, request));
    }

    /** 停用凭证明细。 */
    @AuditLog(logType = "operation", code = "credential_item_disable", name = "停用凭证明细")
    @PostMapping("/items/{id}/disable")
    public ApiResponse<CredentialItemVo> disableItem(@PathVariable("id") Long id) {
        return ApiResponse.success("已停用", credentialAdminService.disableItem(id));
    }

    /** 启用凭证明细。 */
    @AuditLog(logType = "operation", code = "credential_item_enable", name = "启用凭证明细")
    @PostMapping("/items/{id}/enable")
    public ApiResponse<CredentialItemVo> enableItem(@PathVariable("id") Long id) {
        return ApiResponse.success("已启用", credentialAdminService.enableItem(id));
    }

    /** 查看凭证明文。 */
    @AuditLog(logType = "operation", code = "credential_item_reveal", name = "查看凭证明文")
    @PostMapping("/items/{id}/reveal")
    public ApiResponse<CredentialRevealVo> revealItem(@PathVariable("id") Long id, HttpServletRequest servletRequest) {
        CredentialRevealVo vo = new CredentialRevealVo();
        vo.setSecretText(credentialAdminService.revealItem(id, servletRequest));
        return ApiResponse.success("获取成功", vo);
    }

    /** 分页查询导入任务。 */
    @GetMapping("/import-tasks")
    public ApiResponse<PageResponse<CredentialImportTaskVo>> importTasks(@Validated CredentialImportTaskListRequest request) {
        return ApiResponse.success("获取成功", credentialAdminService.listImportTasks(request));
    }

    /** 分页查询兑换记录。 */
    @GetMapping("/redeem-records")
    public ApiResponse<PageResponse<CredentialRedeemRecordVo>> redeemRecords(@Validated CredentialRedeemRecordListRequest request) {
        return ApiResponse.success("获取成功", credentialAdminService.listRedeemRecords(request));
    }

    /**
     * 凭证明文查看结果。
     * 仅用于强审计接口返回敏感明文。
     * 创建日期：2026-05-01
     * author：sunshengxian
     */
    @Data
    public static class CredentialRevealVo {
        private String secretText;
    }
}
