package com.winsalty.quickstart.points.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.points.dto.AdminPointAccountListRequest;
import com.winsalty.quickstart.points.dto.AdminPointLedgerListRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentApproveRequest;
import com.winsalty.quickstart.points.dto.PointAdjustmentRequest;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import com.winsalty.quickstart.points.vo.PointAdjustmentOrderVo;
import com.winsalty.quickstart.points.vo.PointLedgerVo;
import com.winsalty.quickstart.points.vo.PointReconciliationVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端积分控制器。
 * 提供积分账户查询、流水审计、人工调整和对账接口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/points")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPointsController {

    private final PointAccountService pointAccountService;

    public AdminPointsController(PointAccountService pointAccountService) {
        this.pointAccountService = pointAccountService;
    }

    @GetMapping("/accounts")
    public ApiResponse<PageResponse<PointAccountVo>> accounts(@Validated AdminPointAccountListRequest request) {
        return ApiResponse.success("获取成功", pointAccountService.listAccounts(request));
    }

    @GetMapping("/ledger")
    public ApiResponse<PageResponse<PointLedgerVo>> ledger(@Validated AdminPointLedgerListRequest request) {
        return ApiResponse.success("获取成功", pointAccountService.listAdminLedger(request));
    }

    @AuditLog(logType = "operation", code = "points_adjust_apply", name = "积分人工调整申请")
    @PostMapping("/adjustments")
    public ApiResponse<PointAdjustmentOrderVo> createAdjustment(@Valid @RequestBody PointAdjustmentRequest request) {
        return ApiResponse.success("申请已提交", pointAccountService.createAdjustment(request));
    }

    @AuditLog(logType = "operation", code = "points_adjust_approve", name = "积分人工调整审批")
    @PostMapping("/adjustments/{id}/approve")
    public ApiResponse<PointAdjustmentOrderVo> approveAdjustment(@PathVariable("id") Long id,
                                                                 @Valid @RequestBody PointAdjustmentApproveRequest request) {
        return ApiResponse.success("审批已处理", pointAccountService.approveAdjustment(id, request));
    }

    @GetMapping("/reconciliation")
    public ApiResponse<PointReconciliationVo> reconciliation() {
        return ApiResponse.success("获取成功", pointAccountService.reconcile());
    }
}
