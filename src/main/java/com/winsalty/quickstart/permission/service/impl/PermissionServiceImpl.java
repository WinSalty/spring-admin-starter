package com.winsalty.quickstart.permission.service.impl;

import com.winsalty.quickstart.permission.entity.MenuEntity;
import com.winsalty.quickstart.permission.entity.RoleActionEntity;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import com.winsalty.quickstart.permission.service.PermissionService;
import com.winsalty.quickstart.permission.vo.PermissionActionVo;
import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;
import com.winsalty.quickstart.permission.vo.PermissionMenuVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public PermissionBootstrapVo getBootstrap(Long userId, String roleCode) {
        List<MenuEntity> menus = permissionMapper.findMenusByRoleCode(roleCode);
        List<String> routes = permissionMapper.findRouteCodesByRoleCode(roleCode);
        List<RoleActionEntity> actions = permissionMapper.findActionsByRoleCode(roleCode);

        PermissionBootstrapVo response = new PermissionBootstrapVo();
        response.setMenus(buildMenuTree(menus));
        response.setRoutes(routes);
        response.setActions(buildActions(actions));
        return response;
    }

    private List<PermissionActionVo> buildActions(List<RoleActionEntity> actions) {
        List<PermissionActionVo> result = new ArrayList<PermissionActionVo>();
        for (RoleActionEntity action : actions) {
            result.add(new PermissionActionVo(action.getActionCode(), action.getActionName()));
        }
        return result;
    }

    private List<PermissionMenuVo> buildMenuTree(List<MenuEntity> menus) {
        Map<String, PermissionMenuVo> menuMap = new LinkedHashMap<String, PermissionMenuVo>();
        List<PermissionMenuVo> roots = new ArrayList<PermissionMenuVo>();
        for (MenuEntity menu : menus) {
            PermissionMenuVo item = new PermissionMenuVo();
            item.setId(String.valueOf(menu.getId()));
            item.setParentId(menu.getParentId() == null ? null : String.valueOf(menu.getParentId()));
            item.setTitle(menu.getTitle());
            item.setPath(menu.getPath());
            item.setIcon(menu.getIcon());
            item.setOrderNo(menu.getOrderNo());
            item.setType(menu.getMenuType());
            item.setPermissionCode(menu.getPermissionCode());
            item.setHiddenInMenu(menu.getHiddenInMenu());
            item.setRedirect(menu.getRedirect());
            item.setKeepAlive(menu.getKeepAlive());
            item.setExternalLink(menu.getExternalLink());
            item.setBadge(menu.getBadge());
            item.setDisabled(menu.getDisabled());
            menuMap.put(item.getId(), item);
        }
        for (PermissionMenuVo menu : menuMap.values()) {
            if (!StringUtils.hasText(menu.getParentId())) {
                roots.add(menu);
                continue;
            }
            PermissionMenuVo parent = menuMap.get(menu.getParentId());
            if (parent == null) {
                roots.add(menu);
                continue;
            }
            parent.getChildren().add(menu);
        }
        return roots;
    }
}
