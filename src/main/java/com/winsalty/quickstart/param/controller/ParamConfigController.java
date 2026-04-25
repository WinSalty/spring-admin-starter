package com.winsalty.quickstart.param.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.param.dto.ParamListRequest;
import com.winsalty.quickstart.param.dto.ParamSaveRequest;
import com.winsalty.quickstart.param.dto.ParamStatusRequest;
import com.winsalty.quickstart.param.service.ParamConfigService;
import com.winsalty.quickstart.param.vo.ParamConfigVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 参数配置控制器。
 * 提供系统运行参数的分页、详情、保存、状态切换和缓存刷新接口。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/system/params")
public class ParamConfigController {

    private final ParamConfigService paramConfigService;

    public ParamConfigController(ParamConfigService paramConfigService) {
        this.paramConfigService = paramConfigService;
    }

    /**
     * 参数配置分页列表。
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<ParamConfigVo>> list(@Validated ParamListRequest request) {
        return ApiResponse.success("获取成功", paramConfigService.getPage(request));
    }

    /**
     * 参数配置详情。
     */
    @GetMapping("/detail")
    public ApiResponse<ParamConfigVo> detail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", paramConfigService.getDetail(id));
    }

    /**
     * 新增或编辑参数配置，保存前会按 valueType 校验值类型。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<ParamConfigVo> save(@Valid @RequestBody ParamSaveRequest request) {
        return ApiResponse.success("保存成功", paramConfigService.save(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/status")
    public ApiResponse<ParamConfigVo> status(@Valid @RequestBody ParamStatusRequest request) {
        return ApiResponse.success("状态已更新", paramConfigService.updateStatus(request));
    }

    /**
     * 刷新参数配置缓存，把当前启用参数写入 Redis。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/refresh")
    public ApiResponse<Boolean> refreshCache() {
        return ApiResponse.success("缓存已刷新", paramConfigService.refreshCache());
    }
}
