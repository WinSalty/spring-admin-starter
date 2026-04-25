package com.winsalty.quickstart.trade.service.impl;

import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.trade.config.TradeProperties;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCallbackRequest;
import com.winsalty.quickstart.trade.dto.OnlineRechargeCreateRequest;
import com.winsalty.quickstart.trade.service.OnlineRechargeService;
import com.winsalty.quickstart.trade.vo.OnlineRechargeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 在线充值服务实现。
 * 通过本地充值单承接支付渠道回调，验签成功后调用积分账务服务完成幂等入账。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class OnlineRechargeServiceImpl implements OnlineRechargeService {

    private static final Logger log = LoggerFactory.getLogger(OnlineRechargeServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RECHARGE_NO_PREFIX = "OR";
    private static final String REQUEST_SOURCE = "online_pay";
    private static final String CALLBACK_SOURCE = "payment_callback";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_SEPARATOR = "&";
    private static final String SIGNATURE_EQUALS = "=";
    private static final String SECRET_KEY = "callbackSecret";
    private static final String ONLINE_RECHARGE_IDEMPOTENCY_PREFIX = "online-recharge:";
    private static final String SYSTEM_OPERATOR_ID = "system";
    private static final String EMPTY_JSON = "{}";
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long CALLBACK_SECRET_MIN_LENGTH = 32L;

    private final PointRechargeOrderMapper pointRechargeOrderMapper;
    private final PointAccountService pointAccountService;
    private final TradeProperties tradeProperties;

    public OnlineRechargeServiceImpl(PointRechargeOrderMapper pointRechargeOrderMapper,
                                     PointAccountService pointAccountService,
                                     TradeProperties tradeProperties) {
        this.pointRechargeOrderMapper = pointRechargeOrderMapper;
        this.pointAccountService = pointAccountService;
        this.tradeProperties = tradeProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnlineRechargeVo createOrder(Long userId, OnlineRechargeCreateRequest request) {
        validateAmount(request.getAmount());
        // 用户侧创建订单先查幂等键，页面重试或网络重发时复用原充值单。
        PointRechargeOrderEntity existed = pointRechargeOrderMapper.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey());
        if (existed != null) {
            log.info("online recharge create idempotency hit, userId={}, rechargeNo={}", userId, existed.getRechargeNo());
            return toVo(existed);
        }
        PointRechargeOrderEntity entity = new PointRechargeOrderEntity();
        entity.setRechargeNo(createNo());
        entity.setUserId(userId);
        entity.setChannel(PointsConstants.CHANNEL_ONLINE_PAY);
        entity.setAmount(request.getAmount());
        entity.setStatus(PointsConstants.ORDER_STATUS_PROCESSING);
        entity.setExternalNo(entity.getRechargeNo());
        entity.setIdempotencyKey(request.getIdempotencyKey());
        entity.setRequestSnapshot(buildRequestSnapshot(request.getAmount()));
        entity.setResultSnapshot(EMPTY_JSON);
        try {
            pointRechargeOrderMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            // 并发请求可能同时通过前置查询，唯一索引冲突后再查一次保证幂等返回。
            PointRechargeOrderEntity retryExisted = pointRechargeOrderMapper.findByUserIdAndIdempotencyKey(userId, request.getIdempotencyKey());
            if (retryExisted != null) {
                log.info("online recharge create duplicate recovered, userId={}, rechargeNo={}", userId, retryExisted.getRechargeNo());
                return toVo(retryExisted);
            }
            throw exception;
        }
        log.info("online recharge order created, userId={}, rechargeNo={}, amount={}",
                userId, entity.getRechargeNo(), entity.getAmount());
        return toVo(pointRechargeOrderMapper.findByRechargeNo(entity.getRechargeNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnlineRechargeVo handleCallback(OnlineRechargeCallbackRequest request) {
        // 回调先验签再加锁查单，拒绝伪造请求占用订单行锁。
        verifyCallback(request);
        PointRechargeOrderEntity order = pointRechargeOrderMapper.findByRechargeNoForUpdate(request.getRechargeNo());
        if (order == null || !PointsConstants.CHANNEL_ONLINE_PAY.equals(order.getChannel())) {
            throw new BusinessException(ErrorCode.TRADE_RECHARGE_ORDER_NOT_FOUND);
        }
        // 金额必须与本地订单一致，防止渠道回调串单或参数被篡改。
        if (!order.getAmount().equals(request.getAmount())) {
            throw new BusinessException(ErrorCode.POINT_AMOUNT_INVALID);
        }
        if (PointsConstants.ORDER_STATUS_SUCCESS.equals(order.getStatus())) {
            log.info("online recharge callback repeated, rechargeNo={}, externalNo={}", order.getRechargeNo(), request.getExternalNo());
            return toVo(order);
        }
        if (!PointsConstants.ORDER_STATUS_PROCESSING.equals(order.getStatus())
                && !PointsConstants.ORDER_STATUS_CREATED.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.TRADE_RECHARGE_ORDER_STATUS_INVALID);
        }
        String nextStatus = normalizeCallbackStatus(request.getStatus());
        String resultSnapshot = buildCallbackSnapshot(request);
        // 状态更新限定原状态，避免两个回调并发时把终态订单再次改写。
        int updated = pointRechargeOrderMapper.updateCallbackResult(order.getRechargeNo(), nextStatus, request.getExternalNo(), resultSnapshot,
                PointsConstants.ORDER_STATUS_CREATED, PointsConstants.ORDER_STATUS_PROCESSING);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.TRADE_RECHARGE_ORDER_STATUS_INVALID);
        }
        if (PointsConstants.ORDER_STATUS_SUCCESS.equals(nextStatus)) {
            PointChangeCommand command = new PointChangeCommand();
            command.setUserId(order.getUserId());
            command.setAmount(order.getAmount());
            command.setBizType(PointsConstants.BIZ_TYPE_ONLINE_RECHARGE);
            command.setBizNo(order.getRechargeNo());
            command.setIdempotencyKey(ONLINE_RECHARGE_IDEMPOTENCY_PREFIX + order.getRechargeNo());
            command.setOperatorType(PointsConstants.OPERATOR_TYPE_SYSTEM);
            command.setOperatorId(SYSTEM_OPERATOR_ID);
            command.setRemark("在线充值入账");
            // 积分入账幂等键绑定充值单号，渠道重复成功回调不会重复加积分。
            pointAccountService.credit(command);
        }
        log.info("online recharge callback handled, rechargeNo={}, externalNo={}, status={}",
                order.getRechargeNo(), request.getExternalNo(), nextStatus);
        return toVo(pointRechargeOrderMapper.findByRechargeNo(order.getRechargeNo()));
    }

    @Override
    public OnlineRechargeVo getOrder(Long userId, String rechargeNo) {
        PointRechargeOrderEntity order = pointRechargeOrderMapper.findByRechargeNo(rechargeNo);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.TRADE_RECHARGE_ORDER_NOT_FOUND);
        }
        return toVo(order);
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0 || amount > tradeProperties.getMaxRechargePoints()) {
            throw new BusinessException(ErrorCode.POINT_AMOUNT_INVALID);
        }
    }

    private void verifyCallback(OnlineRechargeCallbackRequest request) {
        validateCallbackSecret();
        long now = System.currentTimeMillis();
        long skewMillis = tradeProperties.getCallbackSkewSeconds() * MILLIS_PER_SECOND;
        // 时间戳偏移限制用于降低旧回调报文被重放的窗口。
        if (Math.abs(now - request.getTimestamp()) > skewMillis) {
            throw new BusinessException(ErrorCode.TRADE_CALLBACK_SIGNATURE_INVALID);
        }
        String expected = hmacSha256(buildSignSource(request), tradeProperties.getCallbackSecret());
        if (!expected.equalsIgnoreCase(request.getSignature())) {
            throw new BusinessException(ErrorCode.TRADE_CALLBACK_SIGNATURE_INVALID);
        }
    }

    private void validateCallbackSecret() {
        if (!StringUtils.hasText(tradeProperties.getCallbackSecret())
                || tradeProperties.getCallbackSecret().length() < CALLBACK_SECRET_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.TRADE_CALLBACK_SECRET_MISSING);
        }
    }

    private String normalizeCallbackStatus(String status) {
        if (PointsConstants.ORDER_STATUS_SUCCESS.equals(status)) {
            return PointsConstants.ORDER_STATUS_SUCCESS;
        }
        if (PointsConstants.ORDER_STATUS_FAILED.equals(status)) {
            return PointsConstants.ORDER_STATUS_FAILED;
        }
        throw new BusinessException(ErrorCode.TRADE_RECHARGE_ORDER_STATUS_INVALID);
    }

    private String buildRequestSnapshot(Long amount) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("source", REQUEST_SOURCE);
        snapshot.put("amount", amount);
        return FastJsonUtils.toJsonString(snapshot);
    }

    private String buildCallbackSnapshot(OnlineRechargeCallbackRequest request) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("source", CALLBACK_SOURCE);
        snapshot.put("externalNo", request.getExternalNo());
        snapshot.put("status", request.getStatus());
        snapshot.put("timestamp", request.getTimestamp());
        snapshot.put("nonce", request.getNonce());
        return FastJsonUtils.toJsonString(snapshot);
    }

    private String buildSignSource(OnlineRechargeCallbackRequest request) {
        // 签名原文保持固定字段顺序，必须与支付渠道侧验签约定一致。
        return "amount" + SIGNATURE_EQUALS + request.getAmount()
                + SIGNATURE_SEPARATOR + "externalNo" + SIGNATURE_EQUALS + request.getExternalNo()
                + SIGNATURE_SEPARATOR + "nonce" + SIGNATURE_EQUALS + request.getNonce()
                + SIGNATURE_SEPARATOR + "rechargeNo" + SIGNATURE_EQUALS + request.getRechargeNo()
                + SIGNATURE_SEPARATOR + "status" + SIGNATURE_EQUALS + request.getStatus()
                + SIGNATURE_SEPARATOR + "timestamp" + SIGNATURE_EQUALS + request.getTimestamp();
    }

    private String hmacSha256(String source, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal(source.getBytes(StandardCharsets.UTF_8));
            char[] chars = new char[bytes.length * 2];
            for (int index = 0; index < bytes.length; index++) {
                int value = bytes[index] & BYTE_MASK;
                chars[index * 2] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
                chars[index * 2 + 1] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
            }
            return new String(chars);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.TRADE_CALLBACK_SIGNATURE_INVALID);
        }
    }

    private String createNo() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return RECHARGE_NO_PREFIX + LocalDateTime.now().format(DATE_TIME_FORMATTER)
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    private OnlineRechargeVo toVo(PointRechargeOrderEntity entity) {
        OnlineRechargeVo vo = new OnlineRechargeVo();
        vo.setRechargeNo(entity.getRechargeNo());
        vo.setChannel(entity.getChannel());
        vo.setAmount(entity.getAmount());
        vo.setStatus(entity.getStatus());
        vo.setExternalNo(entity.getExternalNo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
