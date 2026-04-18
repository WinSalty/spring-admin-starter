package com.winsalty.quickstart.permission.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.permission.dto.PermissionAssignmentSaveRequest;
import com.winsalty.quickstart.permission.entity.MenuEntity;
import com.winsalty.quickstart.permission.entity.RoleActionEntity;
import com.winsalty.quickstart.permission.mapper.PermissionMapper;
import com.winsalty.quickstart.permission.service.PermissionService;
import com.winsalty.quickstart.permission.vo.PermissionActionVo;
import com.winsalty.quickstart.permission.vo.PermissionAssignmentVo;
import com.winsalty.quickstart.permission.vo.PermissionBootstrapVo;
import com.winsalty.quickstart.permission.vo.PermissionMenuVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限服务实现。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);
    private static final String BOOTSTRAP_VERSION_KEY = "sa:cache:ver:bootstrap";
    private static final String BOOTSTRAP_CACHE_KEY_PREFIX = "sa:bootstrap:v";
    private static final long BOOTSTRAP_CACHE_TTL_SECONDS = 1800L;

    private final PermissionMapper permissionMapper;
    private final RedisCacheService redisCacheService;

    public PermissionServiceImpl(PermissionMapper permissionMapper,
                                 RedisCacheService redisCacheService) {
        this.permissionMapper = permissionMapper;
        this.redisCacheService = redisCacheService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public PermissionBootstrapVo getBootstrap(Long userId, String roleCode) {
        String actualRoleCode = permissionMapper.findRoleCodeByUserId(userId);
        if (StringUtils.hasText(actualRoleCode)) {
            roleCode = actualRoleCode;
        }
        long version = currentVersion(BOOTSTRAP_VERSION_KEY);
        String cacheKey = BOOTSTRAP_CACHE_KEY_PREFIX + version + ":role:" + roleCode;
        Object cached = redisCacheService.get(cacheKey);
        if (cached instanceof PermissionBootstrapVo) {
            log.info("permission bootstrap cache hit, userId={}, roleCode={}, cacheKey={}", userId, roleCode, cacheKey);
            return (PermissionBootstrapVo) cached;
        }
        List<MenuEntity> menus = permissionMapper.findMenusByRoleCode(roleCode);
        List<String> routes = permissionMapper.findRouteCodesByRoleCode(roleCode);
        List<RoleActionEntity> actions = permissionMapper.findActionsByRoleCode(roleCode);

        PermissionBootstrapVo response = new PermissionBootstrapVo();
        response.setMenus(buildMenuTree(menus));
        response.setRoutes(routes);
        response.setActions(buildActions(actions));
        redisCacheService.set(cacheKey, response, BOOTSTRAP_CACHE_TTL_SECONDS);
        log.info("permission bootstrap cache refreshed, userId={}, roleCode={}, cacheKey={}, menuSize={}, routeSize={}, actionSize={}",
                userId, roleCode, cacheKey, menus.size(), routes.size(), actions.size());
        return response;
    }

    @Override
    public PermissionAssignmentVo getAssignment(String roleCode) {
        Long roleId = permissionMapper.findRoleIdByRoleCode(roleCode);
        if (roleId == null) {
            throw new BusinessException(4043, "角色不存在");
        }
        PermissionAssignmentVo response = new PermissionAssignmentVo();
        response.setRoleCode(roleCode);
        response.setMenuIds(toStringIds(permissionMapper.findMenuIdsByRoleCode(roleCode)));
        response.setRouteCodes(permissionMapper.findRouteCodesByRoleCode(roleCode));
        response.setActionCodes(permissionMapper.findActionCodesByRoleCode(roleCode));
        log.info("permission assignment loaded, roleCode={}, menuSize={}, routeSize={}, actionSize={}",
                roleCode, response.getMenuIds().size(), response.getRouteCodes().size(), response.getActionCodes().size());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionAssignmentVo saveAssignment(PermissionAssignmentSaveRequest request) {
        Long roleId = permissionMapper.findRoleIdByRoleCode(request.getRoleCode());
        if (roleId == null) {
            throw new BusinessException(4043, "角色不存在");
        }
        List<Long> menuIds = parseMenuIds(request.getMenuIds());
        List<MenuEntity> menus = menuIds.isEmpty() ? Collections.<MenuEntity>emptyList() : permissionMapper.findMenusByIds(menuIds);
        if (menus.size() != menuIds.size()) {
            throw new BusinessException(4012, "存在无效菜单ID");
        }
        Set<String> routeCodeSet = new LinkedHashSet<String>(distinctNonBlank(request.getRouteCodes()));
        Set<String> validRouteCodes = new LinkedHashSet<String>(permissionMapper.findAllRouteCodes());
        if (!validRouteCodes.containsAll(routeCodeSet)) {
            throw new BusinessException(4013, "存在未授权的路由权限码");
        }

        permissionMapper.deleteRoleMenus(roleId);
        permissionMapper.deleteRoleActions(roleId);
        permissionMapper.deleteRoleRoutes(roleId);

        for (Long menuId : menuIds) {
            permissionMapper.insertRoleMenu(roleId, menuId);
        }
        for (String routeCode : routeCodeSet) {
            permissionMapper.insertRoleRoute(roleId, routeCode);
        }
        for (String actionCode : distinctNonBlank(request.getActionCodes())) {
            permissionMapper.insertRoleAction(roleId, actionCode, resolveActionName(actionCode));
        }
        long version = nextVersion(BOOTSTRAP_VERSION_KEY);
        log.info("permission assignment saved, roleCode={}, menuSize={}, routeSize={}, actionSize={}, cacheVersion={}",
                request.getRoleCode(), menuIds.size(), routeCodeSet.size(), request.getActionCodes().size(), version);
        return getAssignment(request.getRoleCode());
    }

    private long currentVersion(String versionKey) {
        Object cached = redisCacheService.get(versionKey);
        if (cached instanceof Number) {
            return ((Number) cached).longValue();
        }
        redisCacheService.set(versionKey, 1L, BOOTSTRAP_CACHE_TTL_SECONDS * 24);
        return 1L;
    }

    private long nextVersion(String versionKey) {
        Long version = redisCacheService.increment(versionKey);
        return version == null ? 1L : version.longValue();
    }

    private List<String> distinctNonBlank(List<String> values) {
        Set<String> result = new LinkedHashSet<String>();
        if (values == null) {
            return new ArrayList<String>();
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value.trim());
            }
        }
        return new ArrayList<String>(result);
    }

    private List<Long> parseMenuIds(List<String> menuIds) {
        List<Long> result = new ArrayList<Long>();
        if (menuIds == null) {
            return result;
        }
        Set<Long> deduplicated = new LinkedHashSet<Long>();
        for (String menuId : menuIds) {
            if (!StringUtils.hasText(menuId)) {
                continue;
            }
            try {
                deduplicated.add(Long.valueOf(menuId.trim()));
            } catch (NumberFormatException exception) {
                throw new BusinessException(4012, "存在无效菜单ID");
            }
        }
        result.addAll(deduplicated);
        return result;
    }

    private List<String> toStringIds(List<Long> ids) {
        List<String> result = new ArrayList<String>();
        for (Long id : ids) {
            result.add(String.valueOf(id));
        }
        return result;
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

    private String resolveActionName(String actionCode) {
        if ("query:add".equals(actionCode)) {
            return "新增查询";
        }
        if ("query:edit".equals(actionCode)) {
            return "编辑查询";
        }
        if ("query:delete".equals(actionCode)) {
            return "删除查询";
        }
        if ("query:export".equals(actionCode)) {
            return "导出查询";
        }
        if ("statistics:view".equals(actionCode)) {
            return "查看统计";
        }
        if ("statistics:export".equals(actionCode)) {
            return "导出统计";
        }
        if ("permission:view".equals(actionCode)) {
            return "查看权限";
        }
        if ("permission:assign".equals(actionCode)) {
            return "分配权限";
        }
        if ("system:user:add".equals(actionCode)) {
            return "新增用户";
        }
        if ("system:user:edit".equals(actionCode)) {
            return "编辑用户";
        }
        if ("system:user:status".equals(actionCode)) {
            return "切换用户状态";
        }
        if ("system:user:reset".equals(actionCode)) {
            return "重置密码";
        }
        if ("system:user:assign-role".equals(actionCode)) {
            return "分配角色";
        }
        if ("system:role:add".equals(actionCode)) {
            return "新增角色";
        }
        if ("system:role:edit".equals(actionCode)) {
            return "编辑角色";
        }
        if ("system:role:status".equals(actionCode)) {
            return "切换角色状态";
        }
        if ("system:role:assign-permission".equals(actionCode)) {
            return "分配权限";
        }
        if ("system:menu:add".equals(actionCode)) {
            return "新增菜单";
        }
        if ("system:menu:edit".equals(actionCode)) {
            return "编辑菜单";
        }
        if ("system:menu:status".equals(actionCode)) {
            return "切换菜单状态";
        }
        if ("system:dict:add".equals(actionCode)) {
            return "新增字典";
        }
        if ("system:dict:edit".equals(actionCode)) {
            return "编辑字典";
        }
        if ("system:dict:status".equals(actionCode)) {
            return "切换字典状态";
        }
        if ("system:dict:refresh".equals(actionCode)) {
            return "刷新字典缓存";
        }
        if ("system:config:view".equals(actionCode)) {
            return "查看系统配置";
        }
        if ("system:config:edit".equals(actionCode)) {
            return "编辑系统配置";
        }
        if ("system:department:add".equals(actionCode)) {
            return "新增部门";
        }
        if ("system:department:edit".equals(actionCode)) {
            return "编辑部门";
        }
        if ("system:department:status".equals(actionCode)) {
            return "切换部门状态";
        }
        if ("system:notice:add".equals(actionCode)) {
            return "新增公告";
        }
        if ("system:notice:edit".equals(actionCode)) {
            return "编辑公告";
        }
        if ("system:notice:status".equals(actionCode)) {
            return "切换公告状态";
        }
        return actionCode;
    }
}
