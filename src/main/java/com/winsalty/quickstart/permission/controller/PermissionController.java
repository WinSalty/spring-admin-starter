package com.winsalty.quickstart.permission.controller;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.permission.service.PermissionService;
import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限控制器。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
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
}
