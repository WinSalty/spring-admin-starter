package com.winsalty.quickstart.auth.security;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证响应处理器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(FastJsonUtils.toJsonString(
                ApiResponse.failure(ErrorCode.AUTH_REQUIRED.getCode(), ErrorCode.AUTH_REQUIRED.getMessage())));
    }
}
