package com.winsalty.quickstart.permission.service.impl;

import com.winsalty.quickstart.permission.entity.MenuEntity;
import com.winsalty.quickstart.permission.entity.RoleActionEntity;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权限服务测试。
 * 覆盖用户权益表未初始化时权限 bootstrap 的降级行为，避免登录后权限初始化被缺表阻断。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class PermissionServiceImplTest {

    private static final long USER_ID = 1001L;
    private static final String ROLE_CODE = "viewer";
    private static final String BENEFIT_ROUTE_CODE = "benefit_extra_route";
    private static final String BENEFIT_ACTION_CODE = "benefit:use";

    @Test
    void getBootstrapShouldSkipBenefitPermissionsWhenUserBenefitTableMissing() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PermissionServiceImpl service = new PermissionServiceImpl(permissionMapper);
        when(permissionMapper.findRoleCodeByUserId(USER_ID)).thenReturn(ROLE_CODE);
        when(permissionMapper.findMenusByRoleCode(ROLE_CODE)).thenReturn(Collections.singletonList(menu()));
        when(permissionMapper.findRouteCodesByRoleCode(ROLE_CODE)).thenReturn(new ArrayList<String>(Collections.singletonList("dashboard")));
        when(permissionMapper.findActionsByRoleCode(ROLE_CODE)).thenReturn(new ArrayList<RoleActionEntity>());
        when(permissionMapper.countUserBenefitTable()).thenReturn(0);

        PermissionBootstrapVo result = service.getBootstrap(USER_ID, ROLE_CODE);

        assertEquals(1, result.getMenus().size());
        assertEquals(Collections.singletonList("dashboard"), result.getRoutes());
        assertTrue(result.getActions().isEmpty());
        verify(permissionMapper, never()).findActiveBenefitPermissionCodes(USER_ID);
    }

    @Test
    void getBootstrapShouldMergeBenefitPermissionsWhenUserBenefitTableExists() {
        PermissionMapper permissionMapper = mock(PermissionMapper.class);
        PermissionServiceImpl service = new PermissionServiceImpl(permissionMapper);
        when(permissionMapper.findRoleCodeByUserId(USER_ID)).thenReturn(ROLE_CODE);
        when(permissionMapper.findMenusByRoleCode(ROLE_CODE)).thenReturn(Collections.singletonList(menu()));
        when(permissionMapper.findRouteCodesByRoleCode(ROLE_CODE)).thenReturn(new ArrayList<String>(Collections.singletonList("dashboard")));
        when(permissionMapper.findActionsByRoleCode(ROLE_CODE)).thenReturn(new ArrayList<RoleActionEntity>());
        when(permissionMapper.countUserBenefitTable()).thenReturn(1);
        when(permissionMapper.findActiveBenefitPermissionCodes(USER_ID)).thenReturn(Arrays.asList(BENEFIT_ROUTE_CODE, BENEFIT_ACTION_CODE));

        PermissionBootstrapVo result = service.getBootstrap(USER_ID, ROLE_CODE);

        assertTrue(result.getRoutes().contains(BENEFIT_ROUTE_CODE));
        assertEquals(BENEFIT_ACTION_CODE, result.getActions().get(0).getCode());
    }

    private MenuEntity menu() {
        MenuEntity entity = new MenuEntity();
        entity.setId(1L);
        entity.setTitle("工作台");
        entity.setPath("/dashboard");
        entity.setOrderNo(1);
        entity.setMenuType("menu");
        entity.setRouteCode("dashboard");
        entity.setHiddenInMenu(false);
        entity.setKeepAlive(false);
        entity.setDisabled(false);
        entity.setStatus("active");
        return entity;
    }
}
