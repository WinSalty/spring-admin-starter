package com.winsalty.quickstart.credential.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.credential.dto.CredentialExtractAccessRecordListRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkDisableRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkExtendRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkListRequest;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.vo.CredentialExtractAccessRecordVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCopyVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 管理端凭证提取链接控制器。
 * 提供提取链接列表、详情、复制、停用、延期和访问记录查询接口。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/credentials/extract-links")
public class AdminCredentialExtractLinkController {

    private final CredentialExtractLinkService credentialExtractLinkService;

    public AdminCredentialExtractLinkController(CredentialExtractLinkService credentialExtractLinkService) {
        this.credentialExtractLinkService = credentialExtractLinkService;
    }

    /**
     * 分页查询凭证提取链接。
     */
    @GetMapping
    public ApiResponse<PageResponse<CredentialExtractLinkVo>> links(@Validated CredentialExtractLinkListRequest request) {
        return ApiResponse.success("获取成功", credentialExtractLinkService.listLinks(request));
    }

    /**
     * 查询凭证提取链接详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<CredentialExtractLinkVo> detail(@PathVariable("id") Long id) {
        return ApiResponse.success("获取成功", credentialExtractLinkService.getLink(id));
    }

    /**
     * 查询凭证提取链接包含的明细。
     */
    @GetMapping("/{id}/items")
    public ApiResponse<List<CredentialItemVo>> items(@PathVariable("id") Long id) {
        return ApiResponse.success("获取成功", credentialExtractLinkService.listItems(id));
    }

    /**
     * 查询凭证提取链接访问记录。
     */
    @GetMapping("/{id}/access-records")
    public ApiResponse<PageResponse<CredentialExtractAccessRecordVo>> accessRecords(@PathVariable("id") Long id,
                                                                                    @Validated CredentialExtractAccessRecordListRequest request) {
        return ApiResponse.success("获取成功", credentialExtractLinkService.listAccessRecords(id, request));
    }

    /**
     * 复制凭证提取链接 URL。
     */
    @AuditLog(logType = "operation", code = "credential_extract_link_copy_url", name = "复制凭证提取链接", recordResponse = false)
    @PostMapping("/{id}/copy-url")
    public ApiResponse<CredentialExtractLinkCopyVo> copyUrl(@PathVariable("id") Long id, HttpServletRequest servletRequest) {
        return ApiResponse.success("复制成功", credentialExtractLinkService.copyUrl(id, servletRequest));
    }

    /**
     * 停用凭证提取链接。
     */
    @AuditLog(logType = "operation", code = "credential_extract_link_disable", name = "停用凭证提取链接")
    @PostMapping("/{id}/disable")
    public ApiResponse<CredentialExtractLinkVo> disable(@PathVariable("id") Long id,
                                                        @Valid @RequestBody CredentialExtractLinkDisableRequest request,
                                                        HttpServletRequest servletRequest) {
        return ApiResponse.success("已停用", credentialExtractLinkService.disableLink(id, request, servletRequest));
    }

    /**
     * 延期凭证提取链接。
     */
    @AuditLog(logType = "operation", code = "credential_extract_link_extend", name = "延期凭证提取链接")
    @PostMapping("/{id}/extend")
    public ApiResponse<CredentialExtractLinkVo> extend(@PathVariable("id") Long id,
                                                       @Valid @RequestBody CredentialExtractLinkExtendRequest request,
                                                       HttpServletRequest servletRequest) {
        return ApiResponse.success("延期成功", credentialExtractLinkService.extendLink(id, request, servletRequest));
    }

    /**
     * 补发凭证提取链接。
     */
    @AuditLog(logType = "operation", code = "credential_extract_link_reissue", name = "补发凭证提取链接", recordResponse = false)
    @PostMapping("/{id}/reissue")
    public ApiResponse<CredentialExtractLinkCopyVo> reissue(@PathVariable("id") Long id,
                                                            @Valid @RequestBody CredentialExtractLinkCreateRequest request,
                                                            HttpServletRequest servletRequest) {
        return ApiResponse.success("补发成功", credentialExtractLinkService.reissueLink(id, request, servletRequest));
    }
}
