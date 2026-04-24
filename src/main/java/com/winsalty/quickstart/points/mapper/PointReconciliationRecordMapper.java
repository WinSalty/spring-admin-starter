package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointReconciliationRecordEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

/**
 * 积分对账记录数据访问接口。
 * 每次手工或定时对账后写入汇总结果，便于后续差异追溯。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointReconciliationRecordMapper {

    @Insert("INSERT INTO point_reconciliation_record(reconcile_no, checked_accounts, different_accounts, total_available_diff, total_frozen_diff, status, checked_at) VALUES(#{reconcileNo}, #{checkedAccounts}, #{differentAccounts}, #{totalAvailableDiff}, #{totalFrozenDiff}, #{status}, STR_TO_DATE(#{checkedAt}, '%Y-%m-%d %H:%i:%s'))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointReconciliationRecordEntity entity);
}
