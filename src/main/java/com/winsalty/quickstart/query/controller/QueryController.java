package com.winsalty.quickstart.query.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.query.dto.QueryListRequest;
import com.winsalty.quickstart.query.dto.QuerySaveRequest;
import com.winsalty.quickstart.query.service.QueryService;
import com.winsalty.quickstart.query.vo.QueryRecordVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询配置控制器。
 * 为前端查询管理模板提供列表、详情和保存接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * 查询配置分页列表，支持 keyword/status 过滤。
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<QueryRecordVo>> list(@Validated QueryListRequest request) {
        return ApiResponse.success("获取成功", queryService.getPage(request));
    }

    /**
     * 按前端记录 ID 查询详情，ID 对应数据库中的 record_code。
     */
    @GetMapping("/detail")
    public ApiResponse<QueryRecordVo> detail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", queryService.getDetail(id));
    }

    /**
     * 新增或编辑查询配置。只有管理员可以修改模板配置。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<QueryRecordVo> save(@Validated @RequestBody QuerySaveRequest request) {
        return ApiResponse.success("保存成功", queryService.save(request));
    }
}
