package com.winsalty.quickstart.benefit.service.impl;

import com.winsalty.quickstart.benefit.constant.BenefitConstants;
import com.winsalty.quickstart.benefit.dto.BenefitExchangeRequest;
import com.winsalty.quickstart.benefit.entity.BenefitExchangeOrderEntity;
import com.winsalty.quickstart.benefit.entity.BenefitProductEntity;
import com.winsalty.quickstart.benefit.mapper.BenefitExchangeOrderMapper;
import com.winsalty.quickstart.benefit.mapper.BenefitProductMapper;
import com.winsalty.quickstart.benefit.mapper.UserBenefitMapper;
import com.winsalty.quickstart.benefit.vo.BenefitExchangeOrderVo;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxService;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointFreezeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointFreezeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 权益兑换服务测试。
 * 覆盖积分冻结、权益发放、确认扣减和 outbox 事件写入链路，验证库存不足不触发账务冻结。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class BenefitExchangeServiceImplTest {

    private static final long PRODUCT_ID = 12L;
    private static final long USER_ID = 1001L;
    private static final long COST_POINTS = 300L;
    private static final String IDEMPOTENCY_KEY = "benefit-idem-001";
    private static final String FREEZE_NO = "PF202604250001";

    @Test
    void exchangeShouldFreezeGrantConfirmAndCreateOutboxEvent() {
        BenefitProductMapper productMapper = mock(BenefitProductMapper.class);
        BenefitExchangeOrderMapper orderMapper = mock(BenefitExchangeOrderMapper.class);
        PointFreezeOrderMapper freezeOrderMapper = mock(PointFreezeOrderMapper.class);
        PointAccountService pointAccountService = mock(PointAccountService.class);
        TransactionOutboxService outboxService = mock(TransactionOutboxService.class);
        BenefitExchangeServiceImpl service = newService(productMapper, orderMapper, freezeOrderMapper, pointAccountService, outboxService);
        BenefitProductEntity product = product();
        when(productMapper.findByIdForUpdate(PRODUCT_ID)).thenReturn(product);
        when(productMapper.incrementStockUsed(PRODUCT_ID)).thenReturn(1);
        when(freezeOrderMapper.findByUserIdAndIdempotencyKey(eq(USER_ID), anyString())).thenReturn(freezeOrder());
        when(orderMapper.findByUserIdAndIdempotencyKey(USER_ID, IDEMPOTENCY_KEY)).thenReturn(null, successOrder());

        BenefitExchangeOrderVo result = service.exchange(PRODUCT_ID, USER_ID, request());

        assertEquals(BenefitConstants.ORDER_STATUS_SUCCESS, result.getStatus());
        ArgumentCaptor<PointChangeCommand> freezeCaptor = ArgumentCaptor.forClass(PointChangeCommand.class);
        verify(pointAccountService).freeze(freezeCaptor.capture());
        assertEquals(USER_ID, freezeCaptor.getValue().getUserId());
        assertEquals(COST_POINTS, freezeCaptor.getValue().getAmount());
        assertEquals(PointsConstants.BIZ_TYPE_BENEFIT_EXCHANGE, freezeCaptor.getValue().getBizType());
        verify(pointAccountService).confirmFreeze(eq(FREEZE_NO), any(PointChangeCommand.class));
        verify(orderMapper).updateStatus(anyString(), eq(BenefitConstants.ORDER_STATUS_SUCCESS), eq(""));
        verify(outboxService).createEvent(eq("benefit_exchange"), anyString(), eq("benefit.exchange.success"), anyString());
    }

    @Test
    void exchangeShouldRejectWhenStockIsNotEnoughBeforeFreezingPoints() {
        BenefitProductMapper productMapper = mock(BenefitProductMapper.class);
        PointAccountService pointAccountService = mock(PointAccountService.class);
        BenefitExchangeServiceImpl service = newService(productMapper, mock(BenefitExchangeOrderMapper.class),
                mock(PointFreezeOrderMapper.class), pointAccountService, mock(TransactionOutboxService.class));
        when(productMapper.findByIdForUpdate(PRODUCT_ID)).thenReturn(product());
        when(productMapper.incrementStockUsed(PRODUCT_ID)).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.exchange(PRODUCT_ID, USER_ID, request()));

        verify(pointAccountService, never()).freeze(any(PointChangeCommand.class));
    }

    private BenefitExchangeServiceImpl newService(BenefitProductMapper productMapper,
                                                  BenefitExchangeOrderMapper orderMapper,
                                                  PointFreezeOrderMapper freezeOrderMapper,
                                                  PointAccountService pointAccountService,
                                                  TransactionOutboxService outboxService) {
        return new BenefitExchangeServiceImpl(
                productMapper,
                orderMapper,
                mock(UserBenefitMapper.class),
                freezeOrderMapper,
                pointAccountService,
                outboxService
        );
    }

    private BenefitExchangeRequest request() {
        BenefitExchangeRequest request = new BenefitExchangeRequest();
        request.setIdempotencyKey(IDEMPOTENCY_KEY);
        return request;
    }

    private BenefitProductEntity product() {
        BenefitProductEntity entity = new BenefitProductEntity();
        entity.setId(PRODUCT_ID);
        entity.setProductNo("BP202604250001");
        entity.setProductName("权限包");
        entity.setBenefitType(BenefitConstants.BENEFIT_TYPE_PERMISSION);
        entity.setBenefitCode("perm:test");
        entity.setBenefitName("测试权限");
        entity.setBenefitConfig("{}");
        entity.setCostPoints(COST_POINTS);
        entity.setStockTotal(10);
        entity.setStockUsed(0);
        entity.setValidFrom("2026-04-24 00:00:00");
        entity.setValidTo("2026-04-26 00:00:00");
        entity.setStatus(BenefitConstants.PRODUCT_STATUS_ACTIVE);
        return entity;
    }

    private PointFreezeOrderEntity freezeOrder() {
        PointFreezeOrderEntity entity = new PointFreezeOrderEntity();
        entity.setFreezeNo(FREEZE_NO);
        entity.setUserId(USER_ID);
        entity.setAmount(COST_POINTS);
        entity.setBizType(PointsConstants.BIZ_TYPE_BENEFIT_EXCHANGE);
        entity.setBizNo("BE202604250001");
        entity.setStatus(PointsConstants.FREEZE_STATUS_FROZEN);
        return entity;
    }

    private BenefitExchangeOrderEntity successOrder() {
        BenefitExchangeOrderEntity entity = new BenefitExchangeOrderEntity();
        entity.setId(20L);
        entity.setOrderNo("BE202604250001");
        entity.setUserId(USER_ID);
        entity.setProductId(PRODUCT_ID);
        entity.setProductNo("BP202604250001");
        entity.setBenefitType(BenefitConstants.BENEFIT_TYPE_PERMISSION);
        entity.setBenefitCode("perm:test");
        entity.setCostPoints(COST_POINTS);
        entity.setFreezeNo(FREEZE_NO);
        entity.setStatus(BenefitConstants.ORDER_STATUS_SUCCESS);
        entity.setFailureMessage("");
        entity.setIdempotencyKey(IDEMPOTENCY_KEY);
        return entity;
    }
}
