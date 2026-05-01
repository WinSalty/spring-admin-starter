package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialImportTaskEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 凭证导入任务数据访问接口。
 * 提供导入任务创建、分页和状态更新能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialImportTaskMapper {

    String TASK_SELECT = "SELECT t.id, t.task_no AS taskNo, t.batch_id AS batchId, t.category_id AS categoryId, "
            + "c.category_name AS categoryName, t.delimiter, t.total_rows AS totalRows, t.valid_rows AS validRows, "
            + "t.duplicate_rows AS duplicateRows, t.invalid_rows AS invalidRows, t.import_hash AS importHash, "
            + "t.result_summary AS resultSummary, t.status, t.created_by AS createdBy, "
            + "DATE_FORMAT(t.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(t.updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt "
            + "FROM credential_import_task t LEFT JOIN credential_category c ON c.id = t.category_id ";

    @Select({
            "<script>",
            TASK_SELECT,
            "WHERE 1 = 1 ",
            "<if test='categoryId != null'>AND t.category_id = #{categoryId} </if>",
            "<if test='batchId != null'>AND t.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND t.status = #{status} </if>",
            "ORDER BY t.id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<CredentialImportTaskEntity> findPage(@Param("categoryId") Long categoryId,
                                              @Param("batchId") Long batchId,
                                              @Param("status") String status,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM credential_import_task t WHERE 1 = 1 ",
            "<if test='categoryId != null'>AND t.category_id = #{categoryId} </if>",
            "<if test='batchId != null'>AND t.batch_id = #{batchId} </if>",
            "<if test='status != null and status != \"\"'>AND t.status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("categoryId") Long categoryId,
                   @Param("batchId") Long batchId,
                   @Param("status") String status);

    @Insert("INSERT INTO credential_import_task(task_no, batch_id, category_id, delimiter, total_rows, valid_rows, duplicate_rows, invalid_rows, import_hash, result_summary, status, created_by) "
            + "VALUES(#{taskNo}, #{batchId}, #{categoryId}, #{delimiter}, #{totalRows}, #{validRows}, #{duplicateRows}, #{invalidRows}, #{importHash}, #{resultSummary}, #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialImportTaskEntity entity);

    @Update("UPDATE credential_import_task SET batch_id = #{batchId}, status = #{status}, result_summary = #{resultSummary} WHERE id = #{id}")
    int updateResult(CredentialImportTaskEntity entity);
}
