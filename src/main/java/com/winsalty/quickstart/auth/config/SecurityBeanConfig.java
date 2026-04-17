package com.winsalty.quickstart.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全公共 Bean 配置。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Configuration
public class SecurityBeanConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
