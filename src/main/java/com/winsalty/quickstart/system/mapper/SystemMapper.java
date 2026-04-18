package com.winsalty.quickstart.system.mapper;

import com.winsalty.quickstart.system.entity.SystemMenuEntity;
import com.winsalty.quickstart.system.entity.SystemRecordEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统管理数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface SystemMapper {

    String USERS_SELECT = "SELECT u.id, u.record_code AS recordCode, 'users' AS moduleKey, IFNULL(u.nickname, u.username) AS name, u.username AS code, u.status, u.owner, u.description, "
            + "d.name AS department, u.department_id AS departmentId, "
            + "(SELECT GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.id SEPARATOR ',') FROM sys_user_role ur INNER JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.deleted = 0) AS roleNames, "
            + "(SELECT GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.id SEPARATOR ',') FROM sys_user_role ur INNER JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.deleted = 0) AS roleCodes, DATE_FORMAT(u.last_login_at, '%Y-%m-%d %H:%i:%s') AS lastLoginAt, "
            + "NULL AS dataScope, NULL AS userCount, NULL AS dictType, NULL AS itemCount, NULL AS cacheKey, NULL AS logType, NULL AS target, NULL AS ipAddress, NULL AS result, NULL AS durationMs, "
            + "DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(u.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_user u LEFT JOIN sys_department d ON d.id = u.department_id WHERE u.deleted = 0 ";

    String ROLES_SELECT = "SELECT r.id, r.record_code AS recordCode, 'roles' AS moduleKey, r.role_name AS name, r.role_code AS code, r.status, r.owner, r.description, "
            + "NULL AS department, NULL AS departmentId, NULL AS roleNames, NULL AS roleCodes, NULL AS lastLoginAt, r.data_scope AS dataScope, (SELECT COUNT(1) FROM sys_user_role ur WHERE ur.role_id = r.id) AS userCount, NULL AS dictType, NULL AS itemCount, NULL AS cacheKey, NULL AS logType, NULL AS target, NULL AS ipAddress, NULL AS result, NULL AS durationMs, "
            + "DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(r.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_role r WHERE r.deleted = 0 ";

    String DICTS_SELECT = "SELECT d.id, d.record_code AS recordCode, 'dicts' AS moduleKey, d.name, d.code, d.status, d.owner, d.description, "
            + "NULL AS department, NULL AS departmentId, NULL AS roleNames, NULL AS roleCodes, NULL AS lastLoginAt, NULL AS dataScope, NULL AS userCount, d.dict_type AS dictType, d.item_count AS itemCount, d.cache_key AS cacheKey, NULL AS logType, NULL AS target, NULL AS ipAddress, NULL AS result, NULL AS durationMs, "
            + "DATE_FORMAT(d.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(d.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_dict_record d WHERE d.deleted = 0 ";

    String LOGS_SELECT = "SELECT l.id, l.record_code AS recordCode, 'logs' AS moduleKey, l.name, l.code, l.status, l.owner, l.description, "
            + "NULL AS department, NULL AS departmentId, NULL AS roleNames, NULL AS roleCodes, NULL AS lastLoginAt, NULL AS dataScope, NULL AS userCount, NULL AS dictType, NULL AS itemCount, NULL AS cacheKey, l.log_type AS logType, l.target AS target, l.ip_address AS ipAddress, l.result AS result, l.duration_ms AS durationMs, "
            + "DATE_FORMAT(l.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(l.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_log_record l WHERE l.deleted = 0 ";

    String UNION_ALL_SELECT = USERS_SELECT + " UNION ALL " + ROLES_SELECT + " UNION ALL " + DICTS_SELECT + " UNION ALL " + LOGS_SELECT;
    String WRITABLE_UNION_SELECT = USERS_SELECT + " UNION ALL " + ROLES_SELECT + " UNION ALL " + DICTS_SELECT;
    String MENUS_SELECT = "SELECT m.id, m.record_code AS recordCode, m.parent_id AS parentId, m.title AS name, m.code, m.status, m.owner, m.description, m.menu_type AS menuType, "
            + "m.icon, m.path AS routePath, m.route_code AS routeCode, m.permission_code AS permissionCode, m.external_link AS externalLink, m.order_no AS orderNo, "
            + "DATE_FORMAT(m.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(m.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM sys_menu m ";

    @Select({
            "<script>",
            "SELECT * FROM (",
            UNION_ALL_SELECT,
            ") t ",
            "WHERE t.moduleKey = #{moduleKey} ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(t.name) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.owner) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.description) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "AND t.status = #{status} ",
            "</if>",
            "<if test='moduleKey == \"logs\" and logType != null and logType != \"\"'>",
            "AND t.logType = #{logType} ",
            "</if>",
            "ORDER BY t.updatedAt DESC, t.id DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<SystemRecordEntity> findPage(@Param("moduleKey") String moduleKey,
                                      @Param("keyword") String keyword,
                                      @Param("status") String status,
                                      @Param("logType") String logType,
                                      @Param("offset") int offset,
                                      @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM (",
            UNION_ALL_SELECT,
            ") t ",
            "WHERE t.moduleKey = #{moduleKey} ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(t.name) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.owner) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(t.description) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "AND t.status = #{status} ",
            "</if>",
            "<if test='moduleKey == \"logs\" and logType != null and logType != \"\"'>",
            "AND t.logType = #{logType} ",
            "</if>",
            "</script>"
    })
    long countPage(@Param("moduleKey") String moduleKey,
                   @Param("keyword") String keyword,
                   @Param("status") String status,
                   @Param("logType") String logType);

    @Select("SELECT * FROM (" + UNION_ALL_SELECT + ") t WHERE t.recordCode = #{recordCode} LIMIT 1")
    SystemRecordEntity findByRecordCode(@Param("recordCode") String recordCode);

    @Select("SELECT * FROM (" + WRITABLE_UNION_SELECT + ") t WHERE t.moduleKey = #{moduleKey} AND t.code = #{code} LIMIT 1")
    SystemRecordEntity findWritableByModuleAndCode(@Param("moduleKey") String moduleKey,
                                                   @Param("code") String code);

    @Select({
            "<script>",
            MENUS_SELECT,
            "WHERE 1 = 1 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(m.title) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(m.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(m.description) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(IFNULL(m.path, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(IFNULL(m.permission_code, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "AND m.status = #{status} ",
            "</if>",
            "ORDER BY m.order_no ASC, m.id ASC",
            "</script>"
    })
    List<SystemMenuEntity> findMenus(@Param("keyword") String keyword, @Param("status") String status);

    @Select(MENUS_SELECT + "WHERE m.code = #{code} LIMIT 1")
    SystemMenuEntity findMenuByCode(@Param("code") String code);

    @Select(MENUS_SELECT + "WHERE m.id = #{id} LIMIT 1")
    SystemMenuEntity findMenuById(@Param("id") Long id);

    @Insert("INSERT INTO sys_user(record_code, username, email, password, nickname, status, owner, description, department_id, deleted) VALUES(#{recordCode}, #{code}, CONCAT(#{code}, '@example.com'), #{roleCodes}, #{name}, #{status}, #{owner}, #{description}, #{departmentId}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(SystemRecordEntity entity);

    @Update("UPDATE sys_user SET nickname = #{name}, username = #{code}, status = #{status}, owner = #{owner}, description = #{description}, department_id = #{departmentId} WHERE id = #{id} AND deleted = 0")
    int updateUser(SystemRecordEntity entity);

    @Update("UPDATE sys_user SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateUserStatus(@Param("id") Long id, @Param("status") String status);

    @Insert("INSERT INTO sys_role(record_code, role_code, role_name, status, owner, description, data_scope, deleted) VALUES(#{recordCode}, #{code}, #{name}, #{status}, #{owner}, #{description}, #{dataScope}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRole(SystemRecordEntity entity);

    @Update("UPDATE sys_role SET role_name = #{name}, role_code = #{code}, status = #{status}, owner = #{owner}, description = #{description}, data_scope = #{dataScope} WHERE id = #{id} AND deleted = 0")
    int updateRole(SystemRecordEntity entity);

    @Update("UPDATE sys_role SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateRoleStatus(@Param("id") Long id, @Param("status") String status);

    @Insert("INSERT INTO sys_dict_record(record_code, name, code, status, owner, description, dict_type, item_count, cache_key, deleted) VALUES(#{recordCode}, #{name}, #{code}, #{status}, #{owner}, #{description}, #{dictType}, #{itemCount}, #{cacheKey}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDict(SystemRecordEntity entity);

    @Update("UPDATE sys_dict_record SET name = #{name}, code = #{code}, status = #{status}, owner = #{owner}, description = #{description}, dict_type = #{dictType} WHERE id = #{id} AND deleted = 0")
    int updateDict(SystemRecordEntity entity);

    @Update("UPDATE sys_dict_record SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateDictStatus(@Param("id") Long id, @Param("status") String status);

    @Insert("INSERT INTO sys_menu(record_code, parent_id, title, code, path, icon, order_no, menu_type, route_code, permission_code, hidden_in_menu, redirect, keep_alive, external_link, badge, disabled, status, owner, description) VALUES(#{recordCode}, #{parentId}, #{name}, #{code}, #{routePath}, #{icon}, #{orderNo}, #{menuType}, #{routeCode}, #{permissionCode}, 0, NULL, 1, #{externalLink}, NULL, 0, #{status}, #{owner}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMenu(SystemMenuEntity entity);

    @Update("UPDATE sys_menu SET parent_id = #{parentId}, title = #{name}, code = #{code}, path = #{routePath}, icon = #{icon}, order_no = #{orderNo}, menu_type = #{menuType}, route_code = #{routeCode}, permission_code = #{permissionCode}, external_link = #{externalLink}, status = #{status}, owner = #{owner}, description = #{description} WHERE id = #{id}")
    int updateMenu(SystemMenuEntity entity);

    @Update("UPDATE sys_menu SET status = #{status} WHERE id = #{id}")
    int updateMenuStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} AND deleted = 0 LIMIT 1")
    Long findRoleIdByCode(@Param("roleCode") String roleCode);

    @Select("SELECT id FROM sys_role WHERE role_name = #{roleName} AND deleted = 0 LIMIT 1")
    Long findRoleIdByName(@Param("roleName") String roleName);

    @Select("SELECT role_code FROM sys_role WHERE id IN (SELECT role_id FROM sys_user_role WHERE user_id = #{userId}) ORDER BY id ASC")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteUserRoles(@Param("userId") Long userId);

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT id FROM sys_department WHERE id = #{id} AND deleted = 0 LIMIT 1")
    Long findDepartmentIdById(@Param("id") Long id);

    @Select("SELECT id FROM sys_department WHERE (code = #{keyword} OR name = #{keyword}) AND deleted = 0 ORDER BY id ASC LIMIT 1")
    Long findDepartmentIdByKeyword(@Param("keyword") String keyword);
}
