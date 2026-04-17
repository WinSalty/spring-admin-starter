package com.winsalty.quickstart.permission.controller;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.permission.dto.PermissionAssignmentSaveRequest;
import com.winsalty.quickstart.permission.service.PermissionService;
import com.winsalty.quickstart.permission.vo.PermissionAssignmentVo;
import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;
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
 * 权限控制器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/bootstrap")
    public ApiResponse<PermissionBootstrapVo> bootstrap() {
        AuthUser authUser = AuthContext.get();
        if (authUser == null) {
            throw new BusinessException(4010, "未登录或登录已失效");
        }
        return ApiResponse.success(permissionService.getBootstrap(authUser.getUserId(), authUser.getRoleCode()));
    }

    @GetMapping("/assignment")
    public ApiResponse<PermissionAssignmentVo> assignment(@RequestParam("roleCode") String roleCode) {
        return ApiResponse.success("获取成功", permissionService.getAssignment(roleCode));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/assignment")
    public ApiResponse<PermissionAssignmentVo> saveAssignment(@Valid @RequestBody PermissionAssignmentSaveRequest request) {
        return ApiResponse.success("保存成功", permissionService.saveAssignment(request));
    }
}
