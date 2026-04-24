package com.winsalty.quickstart.trade.service;

import com.winsalty.quickstart.trade.dto.OnlineRechargeCallbackRequest;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCreateRequest;
import com.winsalty.quickstart.trade.vo.OnlineRechargeVo;

/**
 * 在线充值服务接口。
 * 负责充值单创建、支付回调验签、状态推进和积分幂等入账。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface OnlineRechargeService {

    OnlineRechargeVo createOrder(Long userId, OnlineRechargeCreateRequest request);

    OnlineRechargeVo handleCallback(OnlineRechargeCallbackRequest request);

    OnlineRechargeVo getOrder(Long userId, String rechargeNo);
}
