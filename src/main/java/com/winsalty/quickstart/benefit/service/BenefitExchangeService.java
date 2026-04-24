package com.winsalty.quickstart.benefit.service;

import com.winsalty.quickstart.benefit.dto.BenefitExchangeRequest;
import com.winsalty.quickstart.benefit.dto.BenefitOrderListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductSaveRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductStatusRequest;
import com.winsalty.quickstart.benefit.vo.BenefitExchangeOrderVo;
import com.winsalty.quickstart.benefit.vo.BenefitProductVo;
import com.winsalty.quickstart.benefit.vo.UserBenefitVo;
import com.winsalty.quickstart.common.api.PageResponse;

/**
 * 权益兑换服务接口。
 * 统一处理权益商品管理、用户积分兑换和用户权益查询。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
public interface BenefitExchangeService {

    PageResponse<BenefitProductVo> listAvailableProducts(BenefitProductListRequest request);

    BenefitExchangeOrderVo exchange(Long productId, Long userId, BenefitExchangeRequest request);

    PageResponse<BenefitExchangeOrderVo> listCurrentUserOrders(Long userId, BenefitOrderListRequest request);

    PageResponse<UserBenefitVo> listCurrentUserBenefits(Long userId, Integer pageNo, Integer pageSize);

    PageResponse<BenefitProductVo> listAdminProducts(BenefitProductListRequest request);

    BenefitProductVo createProduct(BenefitProductSaveRequest request);

    BenefitProductVo updateProduct(Long id, BenefitProductSaveRequest request);

    BenefitProductVo updateProductStatus(Long id, BenefitProductStatusRequest request);

    PageResponse<BenefitExchangeOrderVo> listAdminOrders(BenefitOrderListRequest request);
}
