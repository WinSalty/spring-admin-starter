package com.winsalty.quickstart.query.mapper;

import com.winsalty.quickstart.query.entity.QueryRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 查询配置数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface QueryMapper {

    @Select({
            "<script>",
            "SELECT q.id, q.record_code AS recordCode, q.name, q.code, q.status, q.owner, q.description, q.call_count AS callCount, ",
            "DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(q.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt ",
            "FROM biz_query_record q ",
            "WHERE q.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(q.name) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.owner) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.description) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "AND q.status = #{status} ",
            "</if>",
            "ORDER BY q.updated_at DESC, q.id DESC ",
            "LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<QueryRecordEntity> findPage(@Param("keyword") String keyword,
                                     @Param("status") String status,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM biz_query_record q ",
            "WHERE q.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(q.name) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.code) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.owner) LIKE CONCAT('%', LOWER(#{keyword}), '%') ",
            "OR LOWER(q.description) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>",
            "AND q.status = #{status} ",
            "</if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword, @Param("status") String status);

    @Select("SELECT q.id, q.record_code AS recordCode, q.name, q.code, q.status, q.owner, q.description, q.call_count AS callCount, DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(q.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM biz_query_record q WHERE q.record_code = #{recordCode} AND q.deleted = 0 LIMIT 1")
    QueryRecordEntity findByRecordCode(@Param("recordCode") String recordCode);

    @Select("SELECT q.id, q.record_code AS recordCode, q.name, q.code, q.status, q.owner, q.description, q.call_count AS callCount, DATE_FORMAT(q.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(q.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM biz_query_record q WHERE q.code = #{code} AND q.deleted = 0 LIMIT 1")
    QueryRecordEntity findByCode(@Param("code") String code);

    @Insert("INSERT INTO biz_query_record(record_code, name, code, status, owner, description, call_count, deleted) VALUES(#{recordCode}, #{name}, #{code}, #{status}, #{owner}, #{description}, #{callCount}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QueryRecordEntity entity);

    @Update("UPDATE biz_query_record SET name = #{name}, code = #{code}, status = #{status}, owner = #{owner}, description = #{description} WHERE id = #{id} AND deleted = 0")
    int update(QueryRecordEntity entity);
}
