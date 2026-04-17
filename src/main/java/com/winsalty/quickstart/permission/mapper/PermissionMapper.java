package com.winsalty.quickstart.permission.mapper;

import com.winsalty.quickstart.permission.entity.MenuEntity;
import com.winsalty.quickstart.permission.entity.RoleActionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface PermissionMapper {

    @Select("SELECT r.role_code FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.id LIMIT 1")
    String findRoleCodeByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_name FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} ORDER BY r.id LIMIT 1")
    String findRoleNameByUserId(@Param("userId") Long userId);

    @Select("SELECT m.id, m.parent_id AS parentId, m.title, m.path, m.icon, m.order_no AS orderNo, m.menu_type AS menuType, m.route_code AS routeCode, m.permission_code AS permissionCode, m.hidden_in_menu AS hiddenInMenu, m.redirect, m.keep_alive AS keepAlive, m.external_link AS externalLink, m.badge, m.disabled, m.status FROM sys_menu m INNER JOIN sys_role_menu rm ON rm.menu_id = m.id INNER JOIN sys_role r ON r.id = rm.role_id WHERE r.role_code = #{roleCode} AND m.status = 'active' ORDER BY m.order_no ASC, m.id ASC")
    List<MenuEntity> findMenusByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT m.route_code FROM sys_menu m INNER JOIN sys_role_menu rm ON rm.menu_id = m.id INNER JOIN sys_role r ON r.id = rm.role_id WHERE r.role_code = #{roleCode} AND m.status = 'active' AND m.route_code IS NOT NULL GROUP BY m.route_code ORDER BY MIN(m.order_no) ASC, MIN(m.id) ASC")
    List<String> findRouteCodesByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT ra.role_id AS roleId, ra.action_code AS actionCode, ra.action_name AS actionName FROM sys_role_action ra INNER JOIN sys_role r ON r.id = ra.role_id WHERE r.role_code = #{roleCode} ORDER BY ra.id ASC")
    List<RoleActionEntity> findActionsByRoleCode(@Param("roleCode") String roleCode);
}
