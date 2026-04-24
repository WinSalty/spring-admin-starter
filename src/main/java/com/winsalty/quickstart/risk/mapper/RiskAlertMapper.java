package com.winsalty.quickstart.risk.mapper;

import com.winsalty.quickstart.risk.entity.RiskAlertEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 风险告警数据访问接口。
 * 支持告警写入和管理端分页查询。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface RiskAlertMapper {

    String ALERT_SELECT = "SELECT id, alert_no AS alertNo, alert_type AS alertType, risk_level AS riskLevel, "
            + "subject_type AS subjectType, subject_no AS subjectNo, user_id AS userId, status, detail_snapshot AS detailSnapshot, "
            + "handled_by AS handledBy, DATE_FORMAT(handled_at, '%Y-%m-%d %H:%i:%s') AS handledAt, "
            + "DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt, DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s') AS updatedAt FROM risk_alert ";

    @Insert("INSERT INTO risk_alert(alert_no, alert_type, risk_level, subject_type, subject_no, user_id, status, detail_snapshot) VALUES(#{alertNo}, #{alertType}, #{riskLevel}, #{subjectType}, #{subjectNo}, #{userId}, #{status}, #{detailSnapshot})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RiskAlertEntity entity);

    @Select({
            "<script>",
            ALERT_SELECT,
            "WHERE 1 = 1 ",
            "<if test='alertType != null and alertType != \"\"'>AND alert_type = #{alertType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<RiskAlertEntity> findPage(@Param("alertType") String alertType,
                                   @Param("status") String status,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM risk_alert WHERE 1 = 1 ",
            "<if test='alertType != null and alertType != \"\"'>AND alert_type = #{alertType} </if>",
            "<if test='status != null and status != \"\"'>AND status = #{status} </if>",
            "</script>"
    })
    long countPage(@Param("alertType") String alertType,
                   @Param("status") String status);
}
