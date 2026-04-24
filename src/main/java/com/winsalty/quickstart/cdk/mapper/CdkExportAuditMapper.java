package com.winsalty.quickstart.cdk.mapper;

import com.winsalty.quickstart.cdk.entity.CdkExportAuditEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * CDK 导出审计数据访问接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface CdkExportAuditMapper {

    @Insert("INSERT INTO cdk_export_audit(batch_id, batch_no, exported_by, export_count, file_fingerprint) VALUES(#{batchId}, #{batchNo}, #{exportedBy}, #{exportCount}, #{fileFingerprint})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CdkExportAuditEntity entity);
}
