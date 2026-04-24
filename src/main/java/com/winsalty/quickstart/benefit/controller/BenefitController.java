package com.winsalty.quickstart.benefit.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.benefit.dto.BenefitExchangeRequest;
import com.winsalty.quickstart.benefit.dto.BenefitOrderListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductListRequest;
import com.winsalty.quickstart.benefit.service.BenefitExchangeService;
import com.winsalty.quickstart.benefit.vo.BenefitExchangeOrderVo;
import com.winsalty.quickstart.benefit.vo.BenefitProductVo;
import com.winsalty.quickstart.benefit.vo.UserBenefitVo;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 用户权益兑换控制器。
 * 提供权益商品列表、积分兑换、兑换订单和已获得权益查询。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/benefits")
public class BenefitController extends BaseController {

    private final BenefitExchangeService benefitExchangeService;

    public BenefitController(BenefitExchangeService benefitExchangeService) {
        this.benefitExchangeService = benefitExchangeService;
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<BenefitProductVo>> products(@Validated BenefitProductListRequest request) {
        return ApiResponse.success("获取成功", benefitExchangeService.listAvailableProducts(request));
    }

    @AuditLog(logType = "business", code = "benefit_exchange", name = "积分兑换权益")
    @PostMapping("/products/{id}/exchange")
    public ApiResponse<BenefitExchangeOrderVo> exchange(@PathVariable("id") Long id,
                                                        @Valid @RequestBody BenefitExchangeRequest request) {
        return ApiResponse.success("兑换成功", benefitExchangeService.exchange(id, currentUserId(), request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<BenefitExchangeOrderVo>> orders(@Validated BenefitOrderListRequest request) {
        return ApiResponse.success("获取成功", benefitExchangeService.listCurrentUserOrders(currentUserId(), request));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<UserBenefitVo>> myBenefits(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                               @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return ApiResponse.success("获取成功", benefitExchangeService.listCurrentUserBenefits(currentUserId(), pageNo, pageSize));
    }
}
