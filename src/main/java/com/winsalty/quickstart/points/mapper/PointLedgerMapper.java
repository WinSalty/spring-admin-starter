package com.winsalty.quickstart.points.mapper;

import com.winsalty.quickstart.points.entity.PointLedgerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 积分流水数据访问接口。
 * 流水只追加不删除，支持幂等查询、分页审计和对账汇总。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Mapper
public interface PointLedgerMapper {

    String LEDGER_SELECT = "SELECT id, ledger_no AS ledgerNo, user_id AS userId, account_id AS accountId, direction, amount, "
            + "balance_before AS balanceBefore, balance_after AS balanceAfter, frozen_before AS frozenBefore, "
            + "frozen_after AS frozenAfter, biz_type AS bizType, biz_no AS bizNo, idempotency_key AS idempotencyKey, "
            + "operator_type AS operatorType, operator_id AS operatorId, trace_id AS traceId, prev_hash AS prevHash, "
            + "entry_hash AS entryHash, remark, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt FROM point_ledger ";

    @Select(LEDGER_SELECT + "WHERE user_id = #{userId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    PointLedgerEntity findByUserIdAndIdempotencyKey(@Param("userId") Long userId, @Param("idempotencyKey") String idempotencyKey);

    @Select(LEDGER_SELECT + "WHERE account_id = #{accountId} ORDER BY id DESC LIMIT 1")
    PointLedgerEntity findLastByAccountId(@Param("accountId") Long accountId);

    @Insert("INSERT INTO point_ledger(ledger_no, user_id, account_id, direction, amount, balance_before, balance_after, frozen_before, frozen_after, biz_type, biz_no, idempotency_key, operator_type, operator_id, trace_id, prev_hash, entry_hash, remark) VALUES(#{ledgerNo}, #{userId}, #{accountId}, #{direction}, #{amount}, #{balanceBefore}, #{balanceAfter}, #{frozenBefore}, #{frozenAfter}, #{bizType}, #{bizNo}, #{idempotencyKey}, #{operatorType}, #{operatorId}, #{traceId}, #{prevHash}, #{entryHash}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PointLedgerEntity entity);

    @Select({
            "<script>",
            LEDGER_SELECT,
            "WHERE user_id = #{userId} ",
            "<if test='direction != null and direction != \"\"'>AND direction = #{direction} </if>",
            "<if test='bizType != null and bizType != \"\"'>AND biz_type = #{bizType} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<PointLedgerEntity> findUserPage(@Param("userId") Long userId,
                                         @Param("direction") String direction,
                                         @Param("bizType") String bizType,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM point_ledger WHERE user_id = #{userId} ",
            "<if test='direction != null and direction != \"\"'>AND direction = #{direction} </if>",
            "<if test='bizType != null and bizType != \"\"'>AND biz_type = #{bizType} </if>",
            "</script>"
    })
    long countUserPage(@Param("userId") Long userId,
                       @Param("direction") String direction,
                       @Param("bizType") String bizType);

    @Select({
            "<script>",
            LEDGER_SELECT,
            "WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='direction != null and direction != \"\"'>AND direction = #{direction} </if>",
            "<if test='bizType != null and bizType != \"\"'>AND biz_type = #{bizType} </if>",
            "<if test='bizNo != null and bizNo != \"\"'>AND biz_no = #{bizNo} </if>",
            "ORDER BY id DESC LIMIT #{offset}, #{pageSize}",
            "</script>"
    })
    List<PointLedgerEntity> findAdminPage(@Param("userId") Long userId,
                                          @Param("direction") String direction,
                                          @Param("bizType") String bizType,
                                          @Param("bizNo") String bizNo,
                                          @Param("offset") int offset,
                                          @Param("pageSize") int pageSize);

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM point_ledger WHERE 1 = 1 ",
            "<if test='userId != null'>AND user_id = #{userId} </if>",
            "<if test='direction != null and direction != \"\"'>AND direction = #{direction} </if>",
            "<if test='bizType != null and bizType != \"\"'>AND biz_type = #{bizType} </if>",
            "<if test='bizNo != null and bizNo != \"\"'>AND biz_no = #{bizNo} </if>",
            "</script>"
    })
    long countAdminPage(@Param("userId") Long userId,
                        @Param("direction") String direction,
                        @Param("bizType") String bizType,
                        @Param("bizNo") String bizNo);

    @Select("SELECT IFNULL(SUM(balance_after - balance_before), 0) FROM point_ledger")
    Long sumAvailableByLedger();

    @Select("SELECT IFNULL(SUM(frozen_after - frozen_before), 0) FROM point_ledger")
    Long sumFrozenByLedger();
}
