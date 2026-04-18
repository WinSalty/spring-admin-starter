package com.winsalty.quickstart.department.mapper;

import com.winsalty.quickstart.department.entity.DepartmentEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门数据访问接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Mapper
public interface DepartmentMapper {

    String DEPARTMENT_SELECT = "SELECT d.id, d.name, d.code, d.parent_id AS parentId, d.sort_order AS sortOrder, d.leader, d.phone, d.email, d.status, d.deleted, "
            + "DATE_FORMAT(d.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(d.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM sys_department d ";

    @Select({
            "<script>",
            DEPARTMENT_SELECT,
            "WHERE d.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(d.name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(d.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(d.leader, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND d.status = #{status} </if>",
            "ORDER BY d.sort_order ASC, d.id ASC",
            "</script>"
    })
    List<DepartmentEntity> findAll(@Param("keyword") String keyword, @Param("status") String status);

    @Select(DEPARTMENT_SELECT + "WHERE d.id = #{id} AND d.deleted = 0 LIMIT 1")
    DepartmentEntity findById(@Param("id") Long id);

    @Select(DEPARTMENT_SELECT + "WHERE d.code = #{code} AND d.deleted = 0 LIMIT 1")
    DepartmentEntity findByCode(@Param("code") String code);

    @Insert("INSERT INTO sys_department(name, code, parent_id, sort_order, leader, phone, email, status, deleted) VALUES(#{name}, #{code}, #{parentId}, #{sortOrder}, #{leader}, #{phone}, #{email}, #{status}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DepartmentEntity entity);

    @Update("UPDATE sys_department SET name = #{name}, code = #{code}, parent_id = #{parentId}, sort_order = #{sortOrder}, leader = #{leader}, phone = #{phone}, email = #{email}, status = #{status} WHERE id = #{id} AND deleted = 0")
    int update(DepartmentEntity entity);

    @Update("UPDATE sys_department SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
