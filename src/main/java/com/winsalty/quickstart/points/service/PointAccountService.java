package com.winsalty.quickstart.points.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.points.dto.AdminPointAccountListRequest;
import com.winsalty.quickstart.points.dto.AdminPointLedgerListRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentApproveRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentRequest;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.dto.PointLedgerListRequest;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import com.winsalty.quickstart.points.vo.PointAdjustmentOrderVo;
import com.winsalty.quickstart.points.vo.PointFreezeOrderVo;
import com.winsalty.quickstart.points.vo.PointLedgerVo;
import com.winsalty.quickstart.points.vo.PointRechargeOrderVo;
import com.winsalty.quickstart.points.vo.PointReconciliationVo;

/**
 * 积分账户服务接口。
 * 业务模块只能通过该接口变更积分，不能直接更新余额表。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface PointAccountService {

    PointAccountVo getOrCreateAccount(Long userId);

    PointLedgerVo credit(PointChangeCommand command);

    PointLedgerVo debit(PointChangeCommand command);

    PointLedgerVo freeze(PointChangeCommand command);

    PointLedgerVo confirmFreeze(String freezeNo, PointChangeCommand command);

    PointLedgerVo cancelFreeze(String freezeNo, PointChangeCommand command);

    PointLedgerVo refund(PointChangeCommand command);

    PageResponse<PointLedgerVo> listCurrentUserLedger(Long userId, PointLedgerListRequest request);

    PageResponse<PointRechargeOrderVo> listCurrentUserRechargeOrders(Long userId, Integer pageNo, Integer pageSize);

    PageResponse<PointLedgerVo> listCurrentUserConsumeOrders(Long userId, Integer pageNo, Integer pageSize);

    PageResponse<PointFreezeOrderVo> listCurrentUserFreezeOrders(Long userId, Integer pageNo, Integer pageSize);

    PageResponse<PointAccountVo> listAccounts(AdminPointAccountListRequest request);

    PageResponse<PointLedgerVo> listAdminLedger(AdminPointLedgerListRequest request);

    PointAdjustmentOrderVo createAdjustment(PointAdjustmentRequest request);

    PointAdjustmentOrderVo approveAdjustment(Long id, PointAdjustmentApproveRequest request);

    PointReconciliationVo reconcile();
}
