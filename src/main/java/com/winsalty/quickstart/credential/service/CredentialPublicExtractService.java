package com.winsalty.quickstart.credential.service;

import com.winsalty.quickstart.credential.dto.CredentialPublicExtractRequest;
import com.winsalty.quickstart.credential.vo.CredentialPublicExtractVo;

import javax.servlet.http.HttpServletRequest;

/**
 * 公开凭证提取服务接口。
 * 定义通过高熵 token 提取一个或多个凭证明文的能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
public interface CredentialPublicExtractService {

    /** 公开提取凭证。 */
    CredentialPublicExtractVo extract(String token, CredentialPublicExtractRequest request, HttpServletRequest servletRequest);
}
