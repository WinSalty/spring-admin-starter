package com.winsalty.quickstart.infra.web;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 请求 TraceId 过滤器。
 * 在请求入口生成或透传 traceId，写入 MDC 并回写响应头，便于日志链路追踪。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        request.setAttribute(TRACE_ID_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private String resolveTraceId(String incomingTraceId) {
        if (StringUtils.hasText(incomingTraceId)) {
            String normalizedTraceId = incomingTraceId.trim();
            if (normalizedTraceId.length() <= 64) {
                return normalizedTraceId;
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
