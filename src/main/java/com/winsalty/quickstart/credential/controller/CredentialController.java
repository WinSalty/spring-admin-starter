package com.winsalty.quickstart.credential.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.credential.dto.CredentialRedeemRequest;
import com.winsalty.quickstart.credential.service.CredentialRedeemService;
import com.winsalty.quickstart.credential.vo.CredentialRedeemVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 用户侧凭证控制器。
 * 提供积分 CDK 兑换入口。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/credentials")
public class CredentialController {

    private final CredentialRedeemService credentialRedeemService;

    public CredentialController(CredentialRedeemService credentialRedeemService) {
        this.credentialRedeemService = credentialRedeemService;
    }

    /** 兑换积分 CDK。 */
    @PostMapping("/redeem")
    public ApiResponse<CredentialRedeemVo> redeem(@Valid @RequestBody CredentialRedeemRequest request,
                                                  HttpServletRequest servletRequest) {
        return ApiResponse.success("兑换成功", credentialRedeemService.redeem(request, servletRequest));
    }
}
