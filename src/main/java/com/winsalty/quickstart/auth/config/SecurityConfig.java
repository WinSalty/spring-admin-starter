package com.winsalty.quickstart.auth.config;

import com.winsalty.quickstart.auth.security.AccessDeniedHandlerImpl;
import com.winsalty.quickstart.auth.security.AuthenticationEntryPointImpl;
import com.winsalty.quickstart.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean swaggerPublicEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          @Value("${app.security.swagger-public-enabled:true}") boolean swaggerPublicEnabled) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.swaggerPublicEnabled = swaggerPublicEnabled;
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
                .requestMatchers(buildPublicMatchers()).permitAll()
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

    private RequestMatcher[] buildPublicMatchers() {
        List<RequestMatcher> matchers = new ArrayList<>();
        matchers.add(new AntPathRequestMatcher("/actuator/health"));
        matchers.add(new AntPathRequestMatcher("/api/common/ping"));
        matchers.add(new AntPathRequestMatcher("/api/auth/login"));
        // refresh-token 虽然和登录态有关，但凭 refresh token 自身校验，不依赖 access token。
        matchers.add(new AntPathRequestMatcher("/api/auth/refresh-token"));
        matchers.add(new AntPathRequestMatcher("/api/auth/register"));
        // 邮件激活链接回调接口必须匿名可访问。
        matchers.add(new AntPathRequestMatcher("/api/auth/register/verify-link"));
        // 头像允许匿名读取，但控制器会进一步校验资源必须为公开、启用且被用户资料引用的头像文件。
        matchers.add(new AntPathRequestMatcher("/api/file/avatar/*"));
        // 本地公共文件需要被浏览器直接加载，私有文件仍走鉴权接口。
        matchers.add(new AntPathRequestMatcher("/api/file/public/**"));
        if (swaggerPublicEnabled) {
            // Swagger 只在显式放开时允许匿名访问，生产环境默认关闭。
            matchers.add(new AntPathRequestMatcher("/swagger-ui.html"));
            matchers.add(new AntPathRequestMatcher("/swagger-ui/**"));
            matchers.add(new AntPathRequestMatcher("/v3/api-docs/**"));
        }
        return matchers.toArray(new RequestMatcher[0]);
    }
}
