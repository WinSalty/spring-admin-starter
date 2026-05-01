package com.winsalty.quickstart.credential.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.credential.dto.CredentialPublicExtractRequest;
import com.winsalty.quickstart.credential.service.CredentialPublicExtractService;
import com.winsalty.quickstart.credential.vo.CredentialPublicExtractVo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 公开凭证控制器。
 * 提供无需登录的提取链接访问入口，依赖高熵 token 完成鉴权。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/public/credentials")
public class PublicCredentialController {

    private final CredentialPublicExtractService credentialPublicExtractService;

    public PublicCredentialController(CredentialPublicExtractService credentialPublicExtractService) {
        this.credentialPublicExtractService = credentialPublicExtractService;
    }

    /** 公开提取凭证。 */
    @PostMapping("/extract/{token}")
    public ApiResponse<CredentialPublicExtractVo> extract(@PathVariable("token") String token,
                                                          @RequestBody(required = false) CredentialPublicExtractRequest request,
                                                          HttpServletRequest servletRequest) {
        return ApiResponse.success("提取成功", credentialPublicExtractService.extract(token, request, servletRequest));
    }
}
