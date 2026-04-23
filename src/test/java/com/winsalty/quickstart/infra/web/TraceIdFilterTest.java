package com.winsalty.quickstart.infra.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TraceId 过滤器测试。
 * 验证请求进入应用时会生成或透传 traceId，并写回响应头。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
class TraceIdFilterTest {

    private final TraceIdFilter traceIdFilter = new TraceIdFilter();

    @Test
    void shouldGenerateTraceIdWhenRequestHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, new MockFilterChain());

        String traceId = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertThat(traceId).isNotBlank();
        assertThat(traceId).hasSize(32);
    }

    @Test
    void shouldReuseIncomingTraceIdWhenHeaderPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-from-gateway");
        MockHttpServletResponse response = new MockHttpServletResponse();

        traceIdFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo("trace-from-gateway");
        assertThat(request.getAttribute(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo("trace-from-gateway");
    }
}
