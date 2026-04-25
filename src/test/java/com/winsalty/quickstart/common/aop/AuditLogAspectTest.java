package com.winsalty.quickstart.common.aop;

import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审计日志切面测试。
 * 验证响应体序列化脱敏和请求参数过滤逻辑。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class AuditLogAspectTest {

    private final AuditLogAspect auditLogAspect = new AuditLogAspect(null);

    /**
     * 响应日志应序列化业务响应体，并对 token 类敏感字段做脱敏。
     */
    @Test
    void responseInfoShouldSerializeApiResponseBodyInsteadOfObjectAddress() {
        LoginResponse loginResponse = new LoginResponse("raw-token", "raw-access-token", "raw-refresh-token", 3600L, 7200L, "Bearer");
        loginResponse.setRoleCode("admin");
        loginResponse.setRoleName("管理员");
        ApiResponse<LoginResponse> response = ApiResponse.success("登录成功", loginResponse);

        Map<String, Object> payload = ReflectionTestUtils.invokeMethod(auditLogAspect, "buildResponsePayload", response, null);
        String responseInfo = ReflectionTestUtils.invokeMethod(auditLogAspect, "safeJson", payload);

        assertThat(responseInfo).contains("\"code\":0");
        assertThat(responseInfo).contains("\"message\":\"登录成功\"");
        assertThat(responseInfo).contains("\"roleCode\":\"admin\"");
        assertThat(responseInfo).doesNotContain("ApiResponse@");
        assertThat(responseInfo).doesNotContain("LoginResponse@");
        assertThat(responseInfo).doesNotContain("raw-token");
        assertThat(responseInfo).doesNotContain("raw-access-token");
        assertThat(responseInfo).doesNotContain("raw-refresh-token");
        assertThat(responseInfo).contains("\"token\":\"***\"");
        assertThat(responseInfo).contains("\"accessToken\":\"***\"");
        assertThat(responseInfo).contains("\"refreshToken\":\"***\"");
    }

    /**
     * 请求日志应跳过 ServletRequest，避免序列化容器对象造成日志膨胀。
     */
    @Test
    void requestArgsShouldSkipServletRequestObject() {
        Object[] args = new Object[]{
                "admin",
                new MockHttpServletRequest()
        };
        Object sanitized = ReflectionTestUtils.invokeMethod(auditLogAspect, "sanitize", (Object) args);
        String requestInfo = ReflectionTestUtils.invokeMethod(auditLogAspect, "safeJson", sanitized);

        assertThat(requestInfo).contains("\"admin\"");
        assertThat(requestInfo).contains("[ignored:MockHttpServletRequest]");
        assertThat(requestInfo).doesNotContain("asyncContext");
    }
}
