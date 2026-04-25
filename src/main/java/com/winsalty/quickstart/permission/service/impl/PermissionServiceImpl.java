package com.winsalty.quickstart.permission.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
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
 * 负责将数据库中的角色菜单、路由码和按钮权限组装成前端可消费的权限模型。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);
    private static final int EXISTING_TABLE_COUNT = 1;
    private final PermissionMapper permissionMapper;

    public PermissionServiceImpl(PermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    /**
     * 构建登录用户的权限 bootstrap 数据。
     * 权限、菜单和用户角色本身支持后台编辑，这里直接读取数据库，避免登录态拿到过期权限。
     */
    @Override
    public PermissionBootstrapVo getBootstrap(Long userId, String roleCode) {
        String actualRoleCode = permissionMapper.findRoleCodeByUserId(userId);
        if (StringUtils.hasText(actualRoleCode)) {
            // token 中的 roleCode 只作为快速载荷，最终以数据库当前角色为准，支持角色调整后立即生效。
            roleCode = actualRoleCode;
        }
        // 菜单、路由、按钮分开读取，前端分别用于侧边栏、路由守卫和按钮级权限判断。
        List<MenuEntity> menus = permissionMapper.findMenusByRoleCode(roleCode);
        List<String> routes = permissionMapper.findRouteCodesByRoleCode(roleCode);
        List<RoleActionEntity> actions = permissionMapper.findActionsByRoleCode(roleCode);
        mergeBenefitPermissions(userId, routes, actions);

        PermissionBootstrapVo response = new PermissionBootstrapVo();
        // menus 给侧边栏和动态路由使用，routes 给前端 RouteGuard 使用，actions 给按钮级 Access 使用。
        response.setMenus(buildMenuTree(menus));
        response.setRoutes(routes);
        response.setActions(buildActions(actions));
        log.info("permission bootstrap loaded, userId={}, roleCode={}, menuSize={}, routeSize={}, actionSize={}",
                userId, roleCode, menus.size(), routes.size(), actions.size());
        return response;
    }

    /**
     * 合并用户通过积分兑换获得的权限码。
     */
    private void mergeBenefitPermissions(Long userId, List<String> routes, List<RoleActionEntity> actions) {
        if (permissionMapper.countUserBenefitTable() < EXISTING_TABLE_COUNT) {
            log.info("user benefit permission skipped because table is not initialized, userId={}", userId);
            return;
        }
        List<String> benefitCodes = permissionMapper.findActiveBenefitPermissionCodes(userId);
        Set<String> routeSet = new LinkedHashSet<String>(routes);
        Set<String> actionSet = new LinkedHashSet<String>();
        for (RoleActionEntity action : actions) {
            // 先收集角色本身的按钮权限，避免权益权限重复追加。
            actionSet.add(action.getActionCode());
        }
        for (String benefitCode : benefitCodes) {
            if (!StringUtils.hasText(benefitCode)) {
                continue;
            }
            if (benefitCode.contains(":")) {
                // 约定带冒号的是按钮/动作权限，例如 query:add；不带冒号的是路由权限。
                if (actionSet.add(benefitCode)) {
                    RoleActionEntity action = new RoleActionEntity();
                    action.setActionCode(benefitCode);
                    action.setActionName(resolveActionName(benefitCode));
                    actions.add(action);
                }
                continue;
            }
            if (routeSet.add(benefitCode)) {
                routes.add(benefitCode);
            }
        }
        log.info("user benefit permissions merged, userId={}, benefitSize={}, routeSize={}, actionSize={}",
                userId, benefitCodes.size(), routes.size(), actions.size());
    }

    /**
     * 读取角色当前权限分配，用于权限分配页面回显。
     */
    @Override
    public PermissionAssignmentVo getAssignment(String roleCode) {
        Long roleId = permissionMapper.findRoleIdByRoleCode(roleCode);
        if (roleId == null) {
            throw new BusinessException(4043, "角色不存在");
        }
        PermissionAssignmentVo response = new PermissionAssignmentVo();
        response.setRoleCode(roleCode);
        // 前端穿梭框使用字符串 id，后端 mapper 保持 Long，出参在这里统一转换。
        response.setMenuIds(toStringIds(permissionMapper.findMenuIdsByRoleCode(roleCode)));
        response.setRouteCodes(permissionMapper.findRouteCodesByRoleCode(roleCode));
        response.setActionCodes(permissionMapper.findActionCodesByRoleCode(roleCode));
        log.info("permission assignment loaded, roleCode={}, menuSize={}, routeSize={}, actionSize={}",
                roleCode, response.getMenuIds().size(), response.getRouteCodes().size(), response.getActionCodes().size());
        return response;
    }

    /**
     * 保存角色权限分配。菜单 ID、路由码会先校验有效性，再整体删除旧关系并重建。
     */
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
            // 菜单必须来自 sys_menu，防止前端提交不存在的 id 造成脏授权关系。
            throw new BusinessException(4012, "存在无效菜单ID");
        }
        Set<String> routeCodeSet = new LinkedHashSet<String>(distinctNonBlank(request.getRouteCodes()));
        Set<String> validRouteCodes = new LinkedHashSet<String>(permissionMapper.findAllRouteCodes());
        if (!validRouteCodes.containsAll(routeCodeSet)) {
            // 路由码必须来自菜单表登记值，避免授权到前端不存在或后端未纳管的路由。
            throw new BusinessException(4013, "存在未授权的路由权限码");
        }

        // 权限分配按“先删后插”整体重建，配合事务保证三张关系表要么全部成功要么全部回滚。
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
            // action 表保留 actionName，便于管理端不依赖前端字典也能展示按钮权限名称。
            permissionMapper.insertRoleAction(roleId, actionCode, resolveActionName(actionCode));
        }
        log.info("permission assignment saved, roleCode={}, menuSize={}, routeSize={}, actionSize={}",
                request.getRoleCode(), menuIds.size(), routeCodeSet.size(), request.getActionCodes().size());
        return getAssignment(request.getRoleCode());
    }

    /**
     * 去除空字符串和重复值，保持请求顺序，避免重复插入关系表。
     */
    private List<String> distinctNonBlank(List<String> values) {
        Set<String> result = new LinkedHashSet<String>();
        if (values == null) {
            return new ArrayList<String>();
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                // LinkedHashSet 在去重的同时保留前端传入顺序，便于排查保存后的权限差异。
                result.add(value.trim());
            }
        }
        return new ArrayList<String>(result);
    }

    /**
     * 前端传入的是字符串 ID，这里统一转换为 Long 并去重。
     */
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
                // 字符串无法转 Long 时直接拒绝，避免 SQL 层隐式转换带来不可控结果。
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
            // VO 只暴露权限码和展示名，不把角色关联表 ID 泄露给前端。
            result.add(new PermissionActionVo(action.getActionCode(), action.getActionName()));
        }
        return result;
    }

    /**
     * 将扁平菜单列表组装成树。若父节点缺失，则把该菜单提升为根节点，避免前端丢菜单。
     */
    private List<PermissionMenuVo> buildMenuTree(List<MenuEntity> menus) {
        Map<String, PermissionMenuVo> menuMap = new LinkedHashMap<String, PermissionMenuVo>();
        List<PermissionMenuVo> roots = new ArrayList<PermissionMenuVo>();
        for (MenuEntity menu : menus) {
            PermissionMenuVo item = new PermissionMenuVo();
            // 后端字段按数据库命名，VO 字段按前端菜单协议命名，在这里集中做协议转换。
            item.setId(String.valueOf(menu.getId()));
            item.setParentId(menu.getParentId() == null ? null : String.valueOf(menu.getParentId()));
            item.setTitle(menu.getTitle());
            item.setPath(menu.getPath());
            item.setIcon(menu.getIcon());
            item.setOrderNo(menu.getOrderNo());
            item.setType(menu.getMenuType());
            item.setRouteCode(menu.getRouteCode());
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
                // 父菜单被禁用或未授权时，把子菜单提升为根节点，避免有权限页面入口被完全隐藏。
                roots.add(menu);
                continue;
            }
            // 保持数据库 ORDER BY 结果顺序追加 children，前端无需再次按 orderNo 排序。
            parent.getChildren().add(menu);
        }
        return roots;
    }

    /**
     * 将按钮权限码转换为展示名。未登记的权限码原样返回，保证扩展权限不会被丢弃。
     */
    private String resolveActionName(String actionCode) {
        // 权限码展示名当前由后端兜底维护；新增动作即使未登记展示名，也会保留原始 actionCode。
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
