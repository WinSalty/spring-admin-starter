package com.winsalty.quickstart.credential.mapper;

import com.winsalty.quickstart.credential.entity.CredentialOperationAuditEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 凭证操作审计数据访问接口。
 * 负责持久化管理员复制、停用和延期等高敏操作。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Mapper
public interface CredentialOperationAuditMapper {

    @Insert("INSERT INTO credential_operation_audit(audit_no, operator_id, operation_type, target_type, target_id, before_snapshot, after_snapshot, client_ip, success, failure_reason) "
            + "VALUES(#{auditNo}, #{operatorId}, #{operationType}, #{targetType}, #{targetId}, #{beforeSnapshot}, #{afterSnapshot}, #{clientIp}, #{success}, #{failureReason})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CredentialOperationAuditEntity entity);
}
