package com.winsalty.quickstart.points.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseController;
import com.winsalty.quickstart.points.dto.PointLedgerListRequest;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointAccountVo;
import com.winsalty.quickstart.points.vo.PointFreezeOrderVo;
import com.winsalty.quickstart.points.vo.PointLedgerVo;
import com.winsalty.quickstart.points.vo.PointRechargeOrderVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户积分控制器。
 * 提供当前用户积分账户、流水和单据查询入口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/points")
public class PointsController extends BaseController {

    private final PointAccountService pointAccountService;

    public PointsController(PointAccountService pointAccountService) {
        this.pointAccountService = pointAccountService;
    }

    @GetMapping("/account")
    public ApiResponse<PointAccountVo> account() {
        return ApiResponse.success("获取成功", pointAccountService.getOrCreateAccount(currentUserId()));
    }

    @GetMapping("/ledger")
    public ApiResponse<PageResponse<PointLedgerVo>> ledger(@Validated PointLedgerListRequest request) {
        return ApiResponse.success("获取成功", pointAccountService.listCurrentUserLedger(currentUserId(), request));
    }

    @GetMapping("/recharge/orders")
    public ApiResponse<PageResponse<PointRechargeOrderVo>> rechargeOrders(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                          @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success("获取成功", pointAccountService.listCurrentUserRechargeOrders(currentUserId(), pageNo, pageSize));
    }

    @GetMapping("/consume/orders")
    public ApiResponse<PageResponse<PointLedgerVo>> consumeOrders(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success("获取成功", pointAccountService.listCurrentUserConsumeOrders(currentUserId(), pageNo, pageSize));
    }

    @GetMapping("/freeze/orders")
    public ApiResponse<PageResponse<PointFreezeOrderVo>> freezeOrders(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                      @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success("获取成功", pointAccountService.listCurrentUserFreezeOrders(currentUserId(), pageNo, pageSize));
    }

}
