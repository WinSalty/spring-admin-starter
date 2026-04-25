package com.winsalty.quickstart.system.dict.mapper;

import com.winsalty.quickstart.system.dict.entity.DictDataEntity;
import com.winsalty.quickstart.system.dict.entity.DictTypeEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 字典 Mapper。
 * 负责 sys_dict_type 和 sys_dict_data 表的数据读写。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
@Mapper
public interface DictMapper {

    String TYPE_COLUMNS = "t.id, t.dict_code AS dictCode, t.dict_name AS dictName, t.dict_type AS dictType, t.status, t.remark, t.deleted, "
            + "DATE_FORMAT(t.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(t.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt";
    String DATA_COLUMNS = "d.id, d.data_code AS dataCode, d.dict_type_id AS dictTypeId, d.dict_type AS dictType, d.label, d.value, d.sort_no AS sortNo, d.status, d.remark, d.deleted, "
            + "DATE_FORMAT(d.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(d.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt";

    @Select({
            "<script>",
            "SELECT ", TYPE_COLUMNS, " FROM sys_dict_type t WHERE t.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(t.dict_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(t.dict_type) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(t.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND t.status = #{status} </if>",
            "ORDER BY t.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<DictTypeEntity> findTypePage(@Param("keyword") String keyword, @Param("status") String status, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_dict_type t WHERE t.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(t.dict_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(t.dict_type) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(t.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND t.status = #{status} </if>",
            "</script>"
    })
    long countTypePage(@Param("keyword") String keyword, @Param("status") String status);

    @Select("SELECT " + TYPE_COLUMNS + " FROM sys_dict_type t WHERE t.id = #{id} AND t.deleted = 0 LIMIT 1")
    DictTypeEntity findTypeById(@Param("id") Long id);

    @Select("SELECT " + TYPE_COLUMNS + " FROM sys_dict_type t WHERE t.dict_type = #{dictType} AND t.deleted = 0 LIMIT 1")
    DictTypeEntity findTypeByDictType(@Param("dictType") String dictType);

    @Insert("INSERT INTO sys_dict_type(dict_code, dict_name, dict_type, status, remark, deleted) VALUES(#{dictCode}, #{dictName}, #{dictType}, #{status}, #{remark}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertType(DictTypeEntity entity);

    @Update("UPDATE sys_dict_type SET dict_name = #{dictName}, dict_type = #{dictType}, status = #{status}, remark = #{remark} WHERE id = #{id} AND deleted = 0")
    int updateType(DictTypeEntity entity);

    @Update("UPDATE sys_dict_type SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateTypeStatus(@Param("id") Long id, @Param("status") String status);

    @Select({
            "<script>",
            "SELECT ", DATA_COLUMNS, " FROM sys_dict_data d WHERE d.deleted = 0 ",
            "<if test='dictType != null and dictType != \"\"'>AND d.dict_type = #{dictType} </if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(d.label) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(d.value) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(d.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND d.status = #{status} </if>",
            "ORDER BY d.dict_type ASC, d.sort_no ASC, d.id ASC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<DictDataEntity> findDataPage(@Param("dictType") String dictType, @Param("keyword") String keyword, @Param("status") String status, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_dict_data d WHERE d.deleted = 0 ",
            "<if test='dictType != null and dictType != \"\"'>AND d.dict_type = #{dictType} </if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(d.label) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(d.value) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(d.remark, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND d.status = #{status} </if>",
            "</script>"
    })
    long countDataPage(@Param("dictType") String dictType, @Param("keyword") String keyword, @Param("status") String status);

    @Select("SELECT " + DATA_COLUMNS + " FROM sys_dict_data d WHERE d.id = #{id} AND d.deleted = 0 LIMIT 1")
    DictDataEntity findDataById(@Param("id") Long id);

    @Select("SELECT " + DATA_COLUMNS + " FROM sys_dict_data d WHERE d.dict_type = #{dictType} AND d.value = #{value} AND d.deleted = 0 LIMIT 1")
    DictDataEntity findDataByTypeAndValue(@Param("dictType") String dictType, @Param("value") String value);

    @Select("SELECT " + DATA_COLUMNS + " FROM sys_dict_data d WHERE d.dict_type = #{dictType} AND d.status = 'active' AND d.deleted = 0 ORDER BY d.sort_no ASC, d.id ASC")
    List<DictDataEntity> findActiveDataByType(@Param("dictType") String dictType);

    @Select("SELECT COUNT(1) FROM sys_dict_data d WHERE d.dict_type = #{dictType} AND d.deleted = 0")
    long countDataByType(@Param("dictType") String dictType);

    @Update("UPDATE sys_dict_data SET dict_type = #{newDictType}, dict_type_id = #{typeId} WHERE dict_type = #{oldDictType} AND deleted = 0")
    int updateDataDictType(@Param("oldDictType") String oldDictType, @Param("newDictType") String newDictType, @Param("typeId") Long typeId);

    @Insert("INSERT INTO sys_dict_data(data_code, dict_type_id, dict_type, label, value, sort_no, status, remark, deleted) VALUES(#{dataCode}, #{dictTypeId}, #{dictType}, #{label}, #{value}, #{sortNo}, #{status}, #{remark}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertData(DictDataEntity entity);

    @Update("UPDATE sys_dict_data SET dict_type_id = #{dictTypeId}, dict_type = #{dictType}, label = #{label}, value = #{value}, sort_no = #{sortNo}, status = #{status}, remark = #{remark} WHERE id = #{id} AND deleted = 0")
    int updateData(DictDataEntity entity);

    @Update("UPDATE sys_dict_data SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateDataStatus(@Param("id") Long id, @Param("status") String status);
}
