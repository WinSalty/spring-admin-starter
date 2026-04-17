package com.winsalty.quickstart.log.mapper;

import com.winsalty.quickstart.log.dto.OperationLogRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 日志数据访问接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Mapper
public interface LogMapper {

    @Insert("INSERT INTO sys_log_record(record_code, name, code, status, owner, description, log_type, target, ip_address, result, duration_ms, deleted) VALUES(#{recordCode}, #{request.name}, #{request.code}, 'active', #{request.owner}, #{request.description}, #{request.logType}, #{request.target}, #{request.ipAddress}, #{request.result}, #{request.durationMs}, 0)")
    int insertLog(@Param("recordCode") String recordCode, @Param("request") OperationLogRequest request);
}
