package com.winsalty.quickstart.common.aop;

import com.winsalty.quickstart.auth.vo.LoginResponse;
import com.winsalty.quickstart.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogAspectTest {

    private final AuditLogAspect auditLogAspect = new AuditLogAspect(null);

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
