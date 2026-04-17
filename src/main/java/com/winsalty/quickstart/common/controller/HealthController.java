package com.winsalty.quickstart.common.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 基础健康检查控制器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/common")
public class HealthController {

    @GetMapping("/ping")
    public ApiResponse<Object> ping() {
        return ApiResponse.success(Collections.singletonMap("status", "UP"));
    }
}
