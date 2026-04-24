package com.winsalty.quickstart.trade.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.trade.config.TradeProperties;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCallbackRequest;
import com.winsalty.quickstart.trade.vo.OnlineRechargeVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 在线充值服务测试。
 * 覆盖支付回调验签、成功入账和重复成功回调幂等，避免重复充值。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class OnlineRechargeServiceImplTest {

    private static final String CALLBACK_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String RECHARGE_NO = "OR202604250001";
    private static final String EXTERNAL_NO = "PAY202604250001";
    private static final String NONCE = "nonce-001";
    private static final long USER_ID = 1001L;
    private static final long AMOUNT = 200L;
    private static final long CALLBACK_SKEW_SECONDS = 300L;

    @Test
    void handleCallbackShouldCreditOnceWhenSuccessCallbackIsRepeated() {
        PointRechargeOrderMapper orderMapper = mock(PointRechargeOrderMapper.class);
        PointAccountService pointAccountService = mock(PointAccountService.class);
        OnlineRechargeServiceImpl service = newService(orderMapper, pointAccountService);
        long timestamp = System.currentTimeMillis();
        OnlineRechargeCallbackRequest request = callbackRequest(timestamp);
        PointRechargeOrderEntity processingOrder = order(PointsConstants.ORDER_STATUS_PROCESSING);
        PointRechargeOrderEntity successOrder = order(PointsConstants.ORDER_STATUS_SUCCESS);
        when(orderMapper.findByRechargeNoForUpdate(RECHARGE_NO)).thenReturn(processingOrder, successOrder);
        when(orderMapper.updateCallbackResult(eq(RECHARGE_NO), eq(PointsConstants.ORDER_STATUS_SUCCESS), eq(EXTERNAL_NO),
                any(), eq(PointsConstants.ORDER_STATUS_CREATED), eq(PointsConstants.ORDER_STATUS_PROCESSING))).thenReturn(1);
        when(orderMapper.findByRechargeNo(RECHARGE_NO)).thenReturn(successOrder);

        OnlineRechargeVo first = service.handleCallback(request);
        OnlineRechargeVo second = service.handleCallback(request);

        assertEquals(PointsConstants.ORDER_STATUS_SUCCESS, first.getStatus());
        assertEquals(PointsConstants.ORDER_STATUS_SUCCESS, second.getStatus());
        ArgumentCaptor<PointChangeCommand> commandCaptor = ArgumentCaptor.forClass(PointChangeCommand.class);
        verify(pointAccountService, times(1)).credit(commandCaptor.capture());
        PointChangeCommand command = commandCaptor.getValue();
        assertEquals(USER_ID, command.getUserId());
        assertEquals(AMOUNT, command.getAmount());
        assertEquals(PointsConstants.BIZ_TYPE_ONLINE_RECHARGE, command.getBizType());
        assertEquals("online-recharge:" + RECHARGE_NO, command.getIdempotencyKey());
    }

    @Test
    void handleCallbackShouldRejectInvalidSignature() {
        PointRechargeOrderMapper orderMapper = mock(PointRechargeOrderMapper.class);
        OnlineRechargeServiceImpl service = newService(orderMapper, mock(PointAccountService.class));
        OnlineRechargeCallbackRequest request = callbackRequest(System.currentTimeMillis());
        request.setSignature("invalid");

        assertThrows(BusinessException.class, () -> service.handleCallback(request));
    }

    private OnlineRechargeServiceImpl newService(PointRechargeOrderMapper orderMapper, PointAccountService pointAccountService) {
        TradeProperties properties = new TradeProperties();
        properties.setCallbackSecret(CALLBACK_SECRET);
        properties.setCallbackSkewSeconds(CALLBACK_SKEW_SECONDS);
        properties.setMaxRechargePoints(100000L);
        return new OnlineRechargeServiceImpl(orderMapper, pointAccountService, properties);
    }

    private OnlineRechargeCallbackRequest callbackRequest(long timestamp) {
        OnlineRechargeCallbackRequest request = new OnlineRechargeCallbackRequest();
        request.setRechargeNo(RECHARGE_NO);
        request.setExternalNo(EXTERNAL_NO);
        request.setStatus(PointsConstants.ORDER_STATUS_SUCCESS);
        request.setAmount(AMOUNT);
        request.setTimestamp(timestamp);
        request.setNonce(NONCE);
        request.setSignature(sign(request));
        return request;
    }

    private PointRechargeOrderEntity order(String status) {
        PointRechargeOrderEntity entity = new PointRechargeOrderEntity();
        entity.setRechargeNo(RECHARGE_NO);
        entity.setUserId(USER_ID);
        entity.setChannel(PointsConstants.CHANNEL_ONLINE_PAY);
        entity.setAmount(AMOUNT);
        entity.setStatus(status);
        entity.setExternalNo(EXTERNAL_NO);
        entity.setIdempotencyKey("idem-001");
        return entity;
    }

    private String sign(OnlineRechargeCallbackRequest request) {
        String source = "amount=" + request.getAmount()
                + "&externalNo=" + request.getExternalNo()
                + "&nonce=" + request.getNonce()
                + "&rechargeNo=" + request.getRechargeNo()
                + "&status=" + request.getStatus()
                + "&timestamp=" + request.getTimestamp();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CALLBACK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(source.getBytes(StandardCharsets.UTF_8));
            char[] hex = "0123456789abcdef".toCharArray();
            char[] chars = new char[bytes.length * 2];
            for (int index = 0; index < bytes.length; index++) {
                int value = bytes[index] & 0xFF;
                chars[index * 2] = hex[value >>> 4];
                chars[index * 2 + 1] = hex[value & 0x0F];
            }
            return new String(chars);
        } catch (Exception exception) {
            throw new IllegalStateException("sign callback failed", exception);
        }
    }
}
