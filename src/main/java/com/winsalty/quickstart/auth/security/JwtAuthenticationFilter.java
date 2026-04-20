package com.winsalty.quickstart.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winsalty.quickstart.auth.service.AuthSessionService;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * JWT 认证过滤器。
 * 从 Authorization: Bearer 请求头解析 access token，并把用户写入 Spring Security 与 AuthContext。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthSessionService authSessionService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   AuthSessionService authSessionService,
                                   ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authSessionService = authSessionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 每个请求只执行一次。finally 中清理上下文，避免 Tomcat 线程复用导致用户串号。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token)) {
                // 这里只接受 access token；refresh token 即使被放在 Authorization 头里也会被 JwtTokenProvider 拒绝。
                AuthUser authUser = jwtTokenProvider.parseAccessToken(token);
                if (!authSessionService.exists(authUser.getSessionId())) {
                    // 登出或同设备新登录会删除旧 session，旧 access token 必须立即失效。
                    throw new BusinessException(ErrorCode.SESSION_INVALID);
                }
                AuthContext.set(authUser);
                // Spring Security 用于鉴权决策，AuthContext 用于业务层直接读取当前用户。
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        authUser.getUsername(),
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + authUser.getRoleCode().toUpperCase()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            if (response.isCommitted()) {
                throw exception;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.failure(exception.getCode(), exception.getMessage())));
        } finally {
            // Web 容器线程会复用，必须清理两个上下文，避免下一个请求继承上一个用户身份。
            AuthContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 只接受 Bearer token，其他认证头一律忽略并交给后续安全链路处理。
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
