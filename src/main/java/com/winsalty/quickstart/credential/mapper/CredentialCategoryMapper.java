package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialCategoryEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 凭证分类数据访问接口。
 * 提供分类配置查询、新增、更新和停用能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialCategoryMapper {

    String CATEGORY_SELECT = "SELECT id, category_code AS categoryCode, category_name AS categoryName, fulfillment_type AS fulfillmentType, "
            + "generation_mode AS generationMode, payload_schema AS payloadSchema, import_config AS importConfig, extract_policy AS extractPolicy, "
            + "status, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM credential_category ";

    @Select(CATEGORY_SELECT + "WHERE status = 'active' ORDER BY id ASC")
    List<CredentialCategoryEntity> findActive();

    @Select(CATEGORY_SELECT + "ORDER BY id ASC")
    List<CredentialCategoryEntity> findAll();

    @Select(CATEGORY_SELECT + "WHERE id = #{id} LIMIT 1")
    CredentialCategoryEntity findById(@Param("id") Long id);

    @Select(CATEGORY_SELECT + "WHERE category_code = #{categoryCode} LIMIT 1")
    CredentialCategoryEntity findByCode(@Param("categoryCode") String categoryCode);

    @Insert("INSERT INTO credential_category(category_code, category_name, fulfillment_type, generation_mode, payload_schema, import_config, extract_policy, status) "
            + "VALUES(#{categoryCode}, #{categoryName}, #{fulfillmentType}, #{generationMode}, #{payloadSchema}, #{importConfig}, #{extractPolicy}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialCategoryEntity entity);

    @Update("UPDATE credential_category SET category_name = #{categoryName}, fulfillment_type = #{fulfillmentType}, generation_mode = #{generationMode}, "
            + "payload_schema = #{payloadSchema}, import_config = #{importConfig}, extract_policy = #{extractPolicy}, status = #{status} WHERE id = #{id}")
    int update(CredentialCategoryEntity entity);

    @Update("UPDATE credential_category SET status = 'disabled' WHERE id = #{id} AND status = 'active'")
    int disable(@Param("id") Long id);
}
