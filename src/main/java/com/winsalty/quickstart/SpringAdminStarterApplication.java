package com.winsalty.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动类。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class SpringAdminStarterApplication {

    public static void main(String[] args) {
        // 项目完全使用自定义 JWT 认证链路，启动时不需要 Spring Security 默认内存用户。
        SpringApplication.run(SpringAdminStarterApplication.class, args);
    }
}
