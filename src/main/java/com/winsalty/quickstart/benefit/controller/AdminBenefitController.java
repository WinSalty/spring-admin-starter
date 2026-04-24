package com.winsalty.quickstart.benefit.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.benefit.dto.BenefitOrderListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductListRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductSaveRequest;
import com.winsalty.quickstart.benefit.dto.BenefitProductStatusRequest;
import com.winsalty.quickstart.benefit.service.BenefitExchangeService;
import com.winsalty.quickstart.benefit.vo.BenefitExchangeOrderVo;
import com.winsalty.quickstart.benefit.vo.BenefitProductVo;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 管理端权益兑换控制器。
 * 提供权益商品管理和兑换订单审计查询。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/admin/benefits")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBenefitController {

    private final BenefitExchangeService benefitExchangeService;

    public AdminBenefitController(BenefitExchangeService benefitExchangeService) {
        this.benefitExchangeService = benefitExchangeService;
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<BenefitProductVo>> products(@Validated BenefitProductListRequest request) {
        return ApiResponse.success("获取成功", benefitExchangeService.listAdminProducts(request));
    }

    @AuditLog(logType = "operation", code = "benefit_product_create", name = "创建权益商品")
    @PostMapping("/products")
    public ApiResponse<BenefitProductVo> createProduct(@Valid @RequestBody BenefitProductSaveRequest request) {
        return ApiResponse.success("创建成功", benefitExchangeService.createProduct(request));
    }

    @AuditLog(logType = "operation", code = "benefit_product_update", name = "更新权益商品")
    @PutMapping("/products/{id}")
    public ApiResponse<BenefitProductVo> updateProduct(@PathVariable("id") Long id,
                                                       @Valid @RequestBody BenefitProductSaveRequest request) {
        return ApiResponse.success("保存成功", benefitExchangeService.updateProduct(id, request));
    }

    @AuditLog(logType = "operation", code = "benefit_product_status", name = "变更权益商品状态")
    @PostMapping("/products/{id}/status")
    public ApiResponse<BenefitProductVo> updateProductStatus(@PathVariable("id") Long id,
                                                             @Valid @RequestBody BenefitProductStatusRequest request) {
        return ApiResponse.success("状态已更新", benefitExchangeService.updateProductStatus(id, request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<BenefitExchangeOrderVo>> orders(@Validated BenefitOrderListRequest request) {
        return ApiResponse.success("获取成功", benefitExchangeService.listAdminOrders(request));
    }
}
