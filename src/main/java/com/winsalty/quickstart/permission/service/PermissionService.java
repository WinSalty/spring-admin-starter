package com.winsalty.quickstart.permission.service;

import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;

/**
 * 权限服务接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
public interface PermissionService {

    PermissionBootstrapVo getBootstrap(Long userId, String roleCode);
}
