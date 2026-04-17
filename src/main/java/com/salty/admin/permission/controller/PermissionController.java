package com.salty.admin.permission.controller;

import com.salty.admin.common.api.ApiResponse;
import com.salty.admin.common.enums.ErrorCode;
import com.salty.admin.common.exception.BusinessException;
import com.salty.admin.common.security.SecurityUtils;
import com.salty.admin.permission.service.PermissionService;
import com.salty.admin.permission.vo.PermissionBootstrapVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping("/bootstrap")
    public ApiResponse<PermissionBootstrapVO> bootstrap() {
        Long userId = SecurityUtils.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.success(permissionService.bootstrap(userId));
    }
}
