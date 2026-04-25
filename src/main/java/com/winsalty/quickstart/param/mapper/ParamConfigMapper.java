package com.winsalty.quickstart.param.mapper;

import com.winsalty.quickstart.param.entity.ParamConfigEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 参数配置 Mapper。
 * 负责 sys_config 表的分页查询、详情查询、保存和状态更新。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Mapper
public interface ParamConfigMapper {
    String COLUMNS = "c.id, c.config_code AS configCode, c.config_name AS configName, c.config_key AS configKey, c.config_value AS configValue, c.value_type AS valueType, c.config_type AS configType, c.status, c.remark, c.deleted, "
            + "DATE_FORMAT(c.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt";

    @Select({
            "<script>",
            "SELECT ", COLUMNS, " FROM sys_config c WHERE c.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(c.config_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(c.config_key) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(c.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='configType != null and configType != \"\"'>AND c.config_type = #{configType} </if>",
            "<if test='status != null and status != \"\"'>AND c.status = #{status} </if>",
            "ORDER BY c.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<ParamConfigEntity> findPage(@Param("keyword") String keyword, @Param("configType") String configType, @Param("status") String status, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_config c WHERE c.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(c.config_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(c.config_key) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(c.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='configType != null and configType != \"\"'>AND c.config_type = #{configType} </if>",
            "<if test='status != null and status != \"\"'>AND c.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword, @Param("configType") String configType, @Param("status") String status);

    @Select("SELECT " + COLUMNS + " FROM sys_config c WHERE c.id = #{id} AND c.deleted = 0 LIMIT 1")
    ParamConfigEntity findById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + " FROM sys_config c WHERE c.config_key = #{configKey} AND c.deleted = 0 LIMIT 1")
    ParamConfigEntity findByKey(@Param("configKey") String configKey);

    @Select("SELECT " + COLUMNS + " FROM sys_config c WHERE c.status = 'active' AND c.deleted = 0 ORDER BY c.id ASC")
    List<ParamConfigEntity> findActiveAll();

    @Insert("INSERT INTO sys_config(config_code, config_name, config_key, config_value, value_type, config_type, status, remark, deleted) VALUES(#{configCode}, #{configName}, #{configKey}, #{configValue}, #{valueType}, #{configType}, #{status}, #{remark}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ParamConfigEntity entity);

    @Update("UPDATE sys_config SET config_name = #{configName}, config_key = #{configKey}, config_value = #{configValue}, value_type = #{valueType}, config_type = #{configType}, status = #{status}, remark = #{remark} WHERE id = #{id} AND deleted = 0")
    int update(ParamConfigEntity entity);

    @Update("UPDATE sys_config SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
