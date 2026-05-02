package com.winsalty.quickstart.log.mapper;

import com.winsalty.quickstart.log.dto.OperationLogRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.Date;

/**
 * 日志数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface LogMapper {

    @Insert("INSERT INTO sys_log_record(record_code, name, code, status, owner, description, log_type, target, ip_address, device_info, user_agent, browser, browser_version, os_name, os_version, device_type, device_brand, request_info, response_info, result, duration_ms, deleted) VALUES(#{recordCode}, #{request.name}, #{request.code}, 'active', #{request.owner}, #{request.description}, #{request.logType}, #{request.target}, #{request.ipAddress}, #{request.deviceInfo}, #{request.userAgent}, #{request.browser}, #{request.browserVersion}, #{request.osName}, #{request.osVersion}, #{request.deviceType}, #{request.deviceBrand}, #{request.requestInfo}, #{request.responseInfo}, #{request.result}, #{request.durationMs}, 0)")
    int insertLog(@Param("recordCode") String recordCode, @Param("request") OperationLogRequest request);

    @Select("SELECT COUNT(1) FROM sys_log_record WHERE deleted = 0 AND created_at < #{cutoffTime}")
    long countArchiveCandidates(@Param("cutoffTime") Date cutoffTime);

    @Insert("INSERT IGNORE INTO sys_log_archive(record_code, name, code, status, owner, description, log_type, target, ip_address, device_info, user_agent, browser, browser_version, os_name, os_version, device_type, device_brand, request_info, response_info, result, duration_ms, deleted, created_at, updated_at, archived_at) SELECT record_code, name, code, status, owner, description, log_type, target, ip_address, device_info, user_agent, browser, browser_version, os_name, os_version, device_type, device_brand, request_info, response_info, result, duration_ms, deleted, created_at, updated_at, NOW() FROM sys_log_record WHERE deleted = 0 AND created_at < #{cutoffTime} ORDER BY id LIMIT #{batchSize}")
    int archiveLogs(@Param("cutoffTime") Date cutoffTime, @Param("batchSize") int batchSize);

    @Delete("DELETE FROM sys_log_record WHERE deleted = 0 AND created_at < #{cutoffTime} ORDER BY id LIMIT #{batchSize}")
    int deleteArchivedLogs(@Param("cutoffTime") Date cutoffTime, @Param("batchSize") int batchSize);
}
