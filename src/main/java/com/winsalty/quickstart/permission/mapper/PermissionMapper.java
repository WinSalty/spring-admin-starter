package com.winsalty.quickstart.permission.mapper;

import com.winsalty.quickstart.permission.entity.MenuEntity;
import com.winsalty.quickstart.permission.entity.RoleActionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
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

    @Select("SELECT r.id FROM sys_role r WHERE r.role_code = #{roleCode} AND r.deleted = 0 LIMIT 1")
    Long findRoleIdByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT r.role_code FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 'active' ORDER BY r.id LIMIT 1")
    String findRoleCodeByUserId(@Param("userId") Long userId);

    // 登录态只取一个主角色；当前 starter 的权限模型按单角色下发菜单和按钮权限。
    @Select("SELECT r.role_name FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.deleted = 0 ORDER BY r.id LIMIT 1")
    String findRoleNameByUserId(@Param("userId") Long userId);

    // 只下发 active 菜单，避免前端渲染已停用入口；排序与系统菜单管理中的 order_no 保持一致。
    @Select("SELECT m.id, m.parent_id AS parentId, m.title, m.path, m.icon, m.order_no AS orderNo, m.menu_type AS menuType, m.route_code AS routeCode, m.permission_code AS permissionCode, m.hidden_in_menu AS hiddenInMenu, m.redirect, m.keep_alive AS keepAlive, m.external_link AS externalLink, m.badge, m.disabled, m.status FROM sys_menu m INNER JOIN sys_role_menu rm ON rm.menu_id = m.id INNER JOIN sys_role r ON r.id = rm.role_id WHERE r.role_code = #{roleCode} AND m.status = 'active' ORDER BY m.order_no ASC, m.id ASC")
    List<MenuEntity> findMenusByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT m.id FROM sys_menu m INNER JOIN sys_role_menu rm ON rm.menu_id = m.id INNER JOIN sys_role r ON r.id = rm.role_id WHERE r.role_code = #{roleCode} ORDER BY m.order_no ASC, m.id ASC")
    List<Long> findMenuIdsByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT rr.route_code FROM sys_role_route rr INNER JOIN sys_role r ON r.id = rr.role_id WHERE r.role_code = #{roleCode} ORDER BY rr.id ASC")
    List<String> findRouteCodesByRoleCode(@Param("roleCode") String roleCode);

    // 保存角色路由权限时以菜单表登记的 route_code 为白名单，杜绝授权不存在的前端路由。
    @Select("SELECT m.route_code FROM sys_menu m WHERE m.route_code IS NOT NULL AND m.route_code != '' GROUP BY m.route_code ORDER BY MIN(m.order_no) ASC, MIN(m.id) ASC")
    List<String> findAllRouteCodes();

    @Select("SELECT ra.role_id AS roleId, ra.action_code AS actionCode, ra.action_name AS actionName FROM sys_role_action ra INNER JOIN sys_role r ON r.id = ra.role_id WHERE r.role_code = #{roleCode} ORDER BY ra.id ASC")
    List<RoleActionEntity> findActionsByRoleCode(@Param("roleCode") String roleCode);

    @Select("SELECT ra.action_code FROM sys_role_action ra INNER JOIN sys_role r ON r.id = ra.role_id WHERE r.role_code = #{roleCode} ORDER BY ra.id ASC")
    List<String> findActionCodesByRoleCode(@Param("roleCode") String roleCode);

    @Select({
            "<script>",
            "SELECT m.id, m.parent_id AS parentId, m.title, m.path, m.icon, m.order_no AS orderNo, m.menu_type AS menuType, ",
            "m.route_code AS routeCode, m.permission_code AS permissionCode, m.hidden_in_menu AS hiddenInMenu, ",
            "m.redirect, m.keep_alive AS keepAlive, m.external_link AS externalLink, m.badge, m.disabled, m.status ",
            "FROM sys_menu m ",
            "WHERE m.id IN ",
            "<foreach collection='menuIds' item='menuId' open='(' separator=',' close=')'>#{menuId}</foreach> ",
            "ORDER BY m.order_no ASC, m.id ASC",
            "</script>"
    })
    List<MenuEntity> findMenusByIds(@Param("menuIds") List<Long> menuIds);

    // 权限保存使用事务包裹，关系表先清空后重建，避免逐条 diff 造成历史脏数据残留。
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenus(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_action WHERE role_id = #{roleId}")
    int deleteRoleActions(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_route WHERE role_id = #{roleId}")
    int deleteRoleRoutes(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Insert("INSERT INTO sys_role_action(role_id, action_code, action_name) VALUES(#{roleId}, #{actionCode}, #{actionName})")
    int insertRoleAction(@Param("roleId") Long roleId,
                         @Param("actionCode") String actionCode,
                         @Param("actionName") String actionName);

    @Insert("INSERT INTO sys_role_route(role_id, route_code) VALUES(#{roleId}, #{routeCode})")
    int insertRoleRoute(@Param("roleId") Long roleId, @Param("routeCode") String routeCode);
}
