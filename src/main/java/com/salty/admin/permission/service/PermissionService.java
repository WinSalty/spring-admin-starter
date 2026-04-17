package com.salty.admin.permission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salty.admin.permission.entity.SysMenu;
import com.salty.admin.permission.entity.SysRole;
import com.salty.admin.permission.mapper.SysMenuMapper;
import com.salty.admin.permission.mapper.SysRoleMapper;
import com.salty.admin.permission.vo.PermissionActionVO;
import com.salty.admin.permission.vo.PermissionBootstrapVO;
import com.salty.admin.permission.vo.PermissionMenuVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private static final String TYPE_BUTTON = "button";

    private final SysRoleMapper roleMapper;

    private final SysMenuMapper menuMapper;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public PermissionService(SysRoleMapper roleMapper,
                             SysMenuMapper menuMapper,
                             StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<SysRole> listRoles(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return roleMapper.selectRolesByUserId(userId);
    }

    public List<SysMenu> listMenus(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return menuMapper.selectMenusByUserId(userId);
    }

    public Set<String> listRoleCodes(Long userId) {
        Set<String> roles = new LinkedHashSet<String>();
        for (SysRole role : listRoles(userId)) {
            roles.add(role.getRoleCode());
        }
        return roles;
    }

    public Set<String> listPermissionCodes(Long userId) {
        Set<String> permissions = new LinkedHashSet<String>();
        for (SysMenu menu : listMenus(userId)) {
            if (StringUtils.hasText(menu.getPermissionCode())) {
                permissions.add(menu.getPermissionCode());
            }
        }
        return permissions;
    }

    public PermissionBootstrapVO bootstrap(Long userId) {
        PermissionBootstrapVO cached = getCachedBootstrap(userId);
        if (cached != null) {
            return cached;
        }
        List<SysMenu> menus = listMenus(userId);
        PermissionBootstrapVO bootstrap = new PermissionBootstrapVO();
        List<PermissionMenuVO> routeMenus = new ArrayList<PermissionMenuVO>();
        List<PermissionActionVO> actions = new ArrayList<PermissionActionVO>();
        Set<String> routes = new LinkedHashSet<String>();

        for (SysMenu menu : menus) {
            if (TYPE_BUTTON.equals(menu.getType())) {
                if (StringUtils.hasText(menu.getPermissionCode())) {
                    actions.add(new PermissionActionVO(menu.getPermissionCode(), menu.getMenuName()));
                }
                continue;
            }
            routeMenus.add(toMenuVO(menu));
            if (StringUtils.hasText(menu.getPermissionCode())) {
                routes.add(menu.getPermissionCode());
            } else if (StringUtils.hasText(menu.getMenuCode())) {
                routes.add(menu.getMenuCode());
            }
        }

        routeMenus.sort(Comparator.comparing(PermissionMenuVO::getOrderNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PermissionMenuVO::getId));
        bootstrap.setMenus(buildTree(routeMenus));
        bootstrap.setRoutes(new ArrayList<String>(routes));
        bootstrap.setActions(actions);
        cacheBootstrap(userId, bootstrap);
        return bootstrap;
    }

    private PermissionBootstrapVO getCachedBootstrap(Long userId) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey(userId));
            if (StringUtils.hasText(value)) {
                return objectMapper.readValue(value, PermissionBootstrapVO.class);
            }
        } catch (Exception ex) {
            log.warn("Read permission bootstrap cache failed, userId={}", userId, ex);
        }
        return null;
    }

    private void cacheBootstrap(Long userId, PermissionBootstrapVO bootstrap) {
        try {
            redisTemplate.opsForValue().set(cacheKey(userId), objectMapper.writeValueAsString(bootstrap), 30, TimeUnit.MINUTES);
        } catch (Exception ex) {
            log.warn("Write permission bootstrap cache failed, userId={}", userId, ex);
        }
    }

    private String cacheKey(Long userId) {
        return "permission:bootstrap:v1:" + userId;
    }

    private PermissionMenuVO toMenuVO(SysMenu menu) {
        PermissionMenuVO vo = new PermissionMenuVO();
        vo.setId(String.valueOf(menu.getId()));
        vo.setParentId(menu.getParentId() == null || menu.getParentId() == 0L ? null : String.valueOf(menu.getParentId()));
        vo.setTitle(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setIcon(menu.getIcon());
        vo.setOrderNo(menu.getSort());
        vo.setType(menu.getType());
        vo.setPermissionCode(StringUtils.hasText(menu.getPermissionCode()) ? menu.getPermissionCode() : menu.getMenuCode());
        vo.setHiddenInMenu(Boolean.TRUE.equals(menu.getHiddenInMenu() != null && menu.getHiddenInMenu() == 1));
        vo.setRedirect(menu.getRedirect());
        vo.setKeepAlive(Boolean.TRUE.equals(menu.getKeepAlive() != null && menu.getKeepAlive() == 1));
        vo.setExternalLink(menu.getExternalLink());
        vo.setBadge(menu.getBadge());
        vo.setDisabled(Boolean.TRUE.equals(menu.getDisabled() != null && menu.getDisabled() == 1));
        return vo;
    }

    private List<PermissionMenuVO> buildTree(List<PermissionMenuVO> source) {
        Map<String, PermissionMenuVO> map = new LinkedHashMap<String, PermissionMenuVO>();
        for (PermissionMenuVO item : source) {
            map.put(item.getId(), item);
        }
        List<PermissionMenuVO> roots = new ArrayList<PermissionMenuVO>();
        Set<String> attached = new HashSet<String>();
        for (PermissionMenuVO item : source) {
            if (item.getParentId() != null && map.containsKey(item.getParentId())) {
                map.get(item.getParentId()).getChildren().add(item);
                attached.add(item.getId());
            }
        }
        for (PermissionMenuVO item : source) {
            if (!attached.contains(item.getId())) {
                roots.add(item);
            }
        }
        sortChildren(roots);
        return roots;
    }

    private void sortChildren(List<PermissionMenuVO> menus) {
        menus.sort(Comparator.comparing(PermissionMenuVO::getOrderNo, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PermissionMenuVO::getId));
        for (PermissionMenuVO menu : menus) {
            sortChildren(menu.getChildren());
        }
    }
}
