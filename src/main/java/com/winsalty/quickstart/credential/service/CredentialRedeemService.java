package com.winsalty.quickstart.credential.service;

import com.winsalty.quickstart.credential.dto.CredentialRedeemRequest;
import com.winsalty.quickstart.credential.vo.CredentialRedeemVo;

import javax.servlet.http.HttpServletRequest;

/**
 * 凭证兑换服务接口。
 * 定义用户提交积分 CDK 并完成积分入账的业务闭环。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
public interface CredentialRedeemService {

    /** 兑换积分 CDK。 */
    CredentialRedeemVo redeem(CredentialRedeemRequest request, HttpServletRequest servletRequest);
}
