package com.winsalty.quickstart.file.mapper;

import com.winsalty.quickstart.file.entity.FileRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FileRecordMapper {
    String COLUMNS = "f.id, f.file_code AS fileCode, f.original_name AS originalName, f.stored_name AS storedName, f.file_path AS filePath, f.storage_type AS storageType, f.bucket_type AS bucketType, f.bucket_name AS bucketName, f.access_policy AS accessPolicy, f.object_key AS objectKey, f.file_url AS fileUrl, f.biz_module AS bizModule, f.biz_id AS bizId, f.visibility, f.owner_type AS ownerType, f.owner_id AS ownerId, f.content_hash AS contentHash, f.content_type AS contentType, f.extension, f.size_bytes AS sizeBytes, f.status, f.deleted, f.created_by AS createdBy, "
            + "DATE_FORMAT(f.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(f.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt";

    @Select({
            "<script>",
            "SELECT ", COLUMNS, " FROM sys_file f WHERE f.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(f.original_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(f.stored_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(f.created_by, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND f.status = #{status} </if>",
            "ORDER BY f.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<FileRecordEntity> findPage(@Param("keyword") String keyword, @Param("status") String status, @Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_file f WHERE f.deleted = 0 ",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND (LOWER(f.original_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(f.stored_name) LIKE CONCAT('%', LOWER(#{keyword}), '%') OR LOWER(IFNULL(f.created_by, '')) LIKE CONCAT('%', LOWER(#{keyword}), '%')) ",
            "</if>",
            "<if test='status != null and status != \"\"'>AND f.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("keyword") String keyword, @Param("status") String status);

    @Select("SELECT " + COLUMNS + " FROM sys_file f WHERE f.id = #{id} AND f.deleted = 0 LIMIT 1")
    FileRecordEntity findById(@Param("id") Long id);

    @Select("SELECT " + COLUMNS + " FROM sys_file f WHERE f.object_key = #{objectKey} AND f.storage_type = 'local' AND f.bucket_type = 'public' AND f.status = 'active' AND f.deleted = 0 ORDER BY f.id ASC LIMIT 1")
    FileRecordEntity findPublicLocalByObjectKey(@Param("objectKey") String objectKey);

    @Select("SELECT " + COLUMNS + " FROM sys_file f WHERE f.content_hash = #{contentHash} AND f.storage_type = #{storageType} AND f.bucket_type = #{bucketType} AND f.access_policy = #{accessPolicy} AND f.status = 'active' AND f.deleted = 0 ORDER BY f.id ASC LIMIT 1")
    FileRecordEntity findReusableByContentHash(@Param("contentHash") String contentHash,
                                               @Param("storageType") String storageType,
                                               @Param("bucketType") String bucketType,
                                               @Param("accessPolicy") String accessPolicy);

    @Select("SELECT " + COLUMNS + " FROM sys_file f WHERE f.content_hash = #{contentHash} AND f.storage_type = 'aliyun-oss' AND f.bucket_type = #{bucketType} AND f.bucket_name = #{bucketName} AND f.access_policy = #{accessPolicy} AND f.status = 'active' AND f.deleted = 0 ORDER BY f.id ASC LIMIT 1")
    FileRecordEntity findReusableAliyunByContentHash(@Param("contentHash") String contentHash,
                                                     @Param("bucketType") String bucketType,
                                                     @Param("bucketName") String bucketName,
                                                     @Param("accessPolicy") String accessPolicy);

    @Insert("INSERT INTO sys_file(file_code, original_name, stored_name, file_path, storage_type, bucket_type, bucket_name, access_policy, object_key, file_url, biz_module, biz_id, visibility, owner_type, owner_id, content_hash, content_type, extension, size_bytes, status, deleted, created_by) VALUES(#{fileCode}, #{originalName}, #{storedName}, #{filePath}, #{storageType}, #{bucketType}, #{bucketName}, #{accessPolicy}, #{objectKey}, #{fileUrl}, #{bizModule}, #{bizId}, #{visibility}, #{ownerType}, #{ownerId}, #{contentHash}, #{contentType}, #{extension}, #{sizeBytes}, #{status}, 0, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FileRecordEntity entity);

    @Update("UPDATE sys_file SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE sys_file SET status = #{status} WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
