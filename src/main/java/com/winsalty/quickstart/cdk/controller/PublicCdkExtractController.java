package com.winsalty.quickstart.cdk.controller;

import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRequest;
import com.winsalty.quickstart.cdk.service.CdkExtractService;
import com.winsalty.quickstart.cdk.vo.CdkExtractViewVo;
import com.winsalty.quickstart.common.api.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 公开 CDK 提取控制器。
 * 匿名访问者通过高熵 token 获取可复制 CDK，并写入访问审计记录。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/public/cdk/extract")
public class PublicCdkExtractController {

    private final CdkExtractService cdkExtractService;

    public PublicCdkExtractController(CdkExtractService cdkExtractService) {
        this.cdkExtractService = cdkExtractService;
    }

    @PostMapping("/{token}")
    public ApiResponse<CdkExtractViewVo> extract(@PathVariable("token") String token,
                                                 @Valid @RequestBody(required = false) CdkExtractAccessRequest request,
                                                 HttpServletRequest servletRequest) {
        CdkExtractAccessRequest safeRequest = request == null ? new CdkExtractAccessRequest() : request;
        return ApiResponse.success("获取成功", cdkExtractService.extract(token, safeRequest, servletRequest));
    }
}
