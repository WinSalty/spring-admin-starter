package com.winsalty.quickstart.auth.config;

import com.winsalty.quickstart.auth.security.AccessDeniedHandlerImpl;
import com.winsalty.quickstart.auth.security.AuthenticationEntryPointImpl;
import com.winsalty.quickstart.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security 基础配置。
 * 使用无状态 JWT 安全链路，公开登录/注册/健康检查等入口，其余接口必须认证。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final RequestMatcher[] PUBLIC_MATCHERS = {
            // 健康检查、文档和登录注册相关入口必须在 JWT 过滤器之前放行。
            new AntPathRequestMatcher("/actuator/health"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/api/common/ping"),
            new AntPathRequestMatcher("/api/common/demo"),
            new AntPathRequestMatcher("/api/auth/login"),
            // refresh-token 虽然和登录态有关，但凭 refresh token 自身校验，不依赖 access token。
            new AntPathRequestMatcher("/api/auth/refresh-token"),
            new AntPathRequestMatcher("/api/auth/register"),
            // 未注册用户无法携带 token，验证码发送接口必须匿名可访问。
            new AntPathRequestMatcher("/api/auth/register/verify-code")
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * 配置核心安全过滤链：CORS、异常处理、无状态 session、公开路径和 JWT 过滤器。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 项目使用无状态 JWT 认证，不依赖 Session/Cookie，不存在 CSRF 攻击面，故禁用
        http.cors().and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                // 认证失败和授权失败分别输出统一 ApiResponse，避免 Spring Security 返回默认 HTML。
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
                .and()
                .authorizeHttpRequests()
                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                .anyRequest().authenticated()
                .and()
                // 自定义 JWT 过滤器必须放在用户名密码过滤器前，先把 Bearer token 转成认证上下文。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPointImpl authenticationEntryPoint() {
        return new AuthenticationEntryPointImpl();
    }

    @Bean
    public AccessDeniedHandlerImpl accessDeniedHandler() {
        return new AccessDeniedHandlerImpl();
    }
}
