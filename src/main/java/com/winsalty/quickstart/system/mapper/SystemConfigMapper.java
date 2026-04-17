package com.winsalty.quickstart.system.mapper;

import com.winsalty.quickstart.system.entity.SystemConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 系统配置数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface SystemConfigMapper {

    @Select("SELECT c.id, c.record_code AS recordCode, c.name, c.code, c.config_type AS configType, c.value_type AS valueType, c.config_value AS configValue, c.description, DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM sys_config_record c WHERE c.deleted = 0 ORDER BY c.config_type ASC, c.id ASC")
    List<SystemConfigEntity> findAll();

    @Select("SELECT c.id, c.record_code AS recordCode, c.name, c.code, c.config_type AS configType, c.value_type AS valueType, c.config_value AS configValue, c.description, DATE_FORMAT(c.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM sys_config_record c WHERE c.record_code = #{recordCode} AND c.deleted = 0 LIMIT 1")
    SystemConfigEntity findByRecordCode(@Param("recordCode") String recordCode);

    @Update("UPDATE sys_config_record SET config_value = #{configValue} WHERE id = #{id} AND deleted = 0")
    int updateValue(@Param("id") Long id, @Param("configValue") String configValue);
}
