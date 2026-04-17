package com.winsalty.quickstart.common.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

/**
 * 基础示例控制器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@RestController
@RequestMapping("/api/common")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/demo")
    public ApiResponse<Object> demo() {
        log.info("demo endpoint invoked");
        return ApiResponse.success(Collections.singletonMap("message", "stage0 ready"));
    }
}
