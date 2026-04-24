package com.winsalty.quickstart.trade.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.base.BaseController;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCallbackRequest;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCreateRequest;
import com.winsalty.quickstart.trade.service.OnlineRechargeService;
import com.winsalty.quickstart.trade.vo.OnlineRechargeVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 在线充值控制器。
 * 提供用户创建充值单、查询充值状态和支付渠道回调入口。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/trade/recharge")
public class OnlineRechargeController extends BaseController {

    private final OnlineRechargeService onlineRechargeService;

    public OnlineRechargeController(OnlineRechargeService onlineRechargeService) {
        this.onlineRechargeService = onlineRechargeService;
    }

    @AuditLog(logType = "business", code = "online_recharge_create", name = "创建在线充值单", recordRequest = false)
    @PostMapping("/orders")
    public ApiResponse<OnlineRechargeVo> createOrder(@Valid @RequestBody OnlineRechargeCreateRequest request) {
        return ApiResponse.success("创建成功", onlineRechargeService.createOrder(currentUserId(), request));
    }

    @GetMapping("/orders/{rechargeNo}")
    public ApiResponse<OnlineRechargeVo> getOrder(@PathVariable("rechargeNo") String rechargeNo) {
        return ApiResponse.success("获取成功", onlineRechargeService.getOrder(currentUserId(), rechargeNo));
    }

    @AuditLog(logType = "business", code = "online_recharge_callback", name = "在线充值回调", recordRequest = false)
    @PostMapping("/callback")
    public ApiResponse<OnlineRechargeVo> callback(@Valid @RequestBody OnlineRechargeCallbackRequest request) {
        return ApiResponse.success("处理成功", onlineRechargeService.handleCallback(request));
    }
}
