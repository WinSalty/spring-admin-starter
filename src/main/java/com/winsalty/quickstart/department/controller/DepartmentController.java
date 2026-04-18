package com.winsalty.quickstart.department.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.department.dto.DepartmentSaveRequest;
import com.winsalty.quickstart.department.dto.DepartmentStatusRequest;
import com.winsalty.quickstart.department.service.DepartmentService;
import com.winsalty.quickstart.department.vo.DepartmentVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;

/**
 * 部门管理控制器。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/system/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<DepartmentVo>> tree(@RequestParam(value = "keyword", required = false) String keyword,
                                                @RequestParam(value = "status", required = false) @Pattern(regexp = "active|disabled", message = "状态值不合法") String status) {
        return ApiResponse.success("获取成功", departmentService.getTree(keyword, status));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<DepartmentVo> save(@Valid @RequestBody DepartmentSaveRequest request) {
        return ApiResponse.success("保存成功", departmentService.save(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/status")
    public ApiResponse<DepartmentVo> status(@Valid @RequestBody DepartmentStatusRequest request) {
        return ApiResponse.success("状态已更新", departmentService.updateStatus(request));
    }
}
