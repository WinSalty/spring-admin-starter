package com.winsalty.quickstart.credential.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialRedeemRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.entity.CredentialRedeemRecordEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialRedeemRecordMapper;
import com.winsalty.quickstart.credential.service.CredentialRedeemService;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import com.winsalty.quickstart.credential.vo.CredentialRedeemVo;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.points.dto.PointChangeCommand;
import com.winsalty.quickstart.points.service.PointAccountService;
import com.winsalty.quickstart.points.vo.PointLedgerVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 凭证兑换服务实现。
 * 按凭证 HMAC 定位积分 CDK，在同一事务中更新凭证状态、写兑换记录并调用积分账务入账。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Service
public class CredentialRedeemServiceImpl extends BaseService implements CredentialRedeemService {

    private static final Logger log = LoggerFactory.getLogger(CredentialRedeemServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RECORD_NO_PREFIX = "CRR";
    private static final String REDIS_LIMIT_USER_PREFIX = "sa:credential:redeem:limit:user:";
    private static final String REDIS_LIMIT_IP_PREFIX = "sa:credential:redeem:limit:ip:";
    private static final String REDIS_FAIL_PREFIX = "sa:credential:redeem:fail:";
    private static final String REDIS_LOCK_PREFIX = "sa:credential:redeem:lock:";
    private static final String OPERATOR_TYPE_USER = "user";
    private static final String FAILURE_INVALID = "invalid";
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final CredentialItemMapper credentialItemMapper;
    private final CredentialBatchMapper credentialBatchMapper;
    private final CredentialRedeemRecordMapper credentialRedeemRecordMapper;
    private final CredentialCryptoService credentialCryptoService;
    private final PointAccountService pointAccountService;
    private final RedisCacheService redisCacheService;
    private final com.winsalty.quickstart.credential.config.CredentialProperties credentialProperties;

    public CredentialRedeemServiceImpl(CredentialItemMapper credentialItemMapper,
                                       CredentialBatchMapper credentialBatchMapper,
                                       CredentialRedeemRecordMapper credentialRedeemRecordMapper,
                                       CredentialCryptoService credentialCryptoService,
                                       PointAccountService pointAccountService,
                                       RedisCacheService redisCacheService,
                                       com.winsalty.quickstart.credential.config.CredentialProperties credentialProperties) {
        this.credentialItemMapper = credentialItemMapper;
        this.credentialBatchMapper = credentialBatchMapper;
        this.credentialRedeemRecordMapper = credentialRedeemRecordMapper;
        this.credentialCryptoService = credentialCryptoService;
        this.pointAccountService = pointAccountService;
        this.redisCacheService = redisCacheService;
        this.credentialProperties = credentialProperties;
    }

    /**
     * 兑换积分 CDK。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialRedeemVo redeem(CredentialRedeemRequest request, HttpServletRequest servletRequest) {
        Long userId = currentUserId();
        String clientIp = IpUtils.getClientIp(servletRequest);
        checkRateLimit(userId, clientIp);
        CredentialRedeemRecordEntity existed = credentialRedeemRecordMapper.findByUserAndIdempotency(userId, request.getIdempotencyKey());
        if (existed != null && CredentialConstants.RECORD_STATUS_SUCCESS.equals(existed.getStatus())) {
            return toRedeemVo(existed, null);
        }
        if (existed != null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_SECRET_INVALID, "该幂等键已兑换失败，请更换后重试");
        }
        String secretText = request.getSecretText().trim();
        CredentialItemEntity item = credentialItemMapper.findBySecretHash(credentialCryptoService.hmacSecret(secretText));
        if (item == null) {
            recordFailure(userId, request, clientIp, servletRequest, null, null, 0L);
            throw new BusinessException(ErrorCode.CREDENTIAL_SECRET_INVALID);
        }
        CredentialBatchEntity batch = credentialBatchMapper.findByIdForUpdate(item.getBatchId());
        try {
            validateRedeemItem(item, batch);
            long points = resolvePoints(item, batch);
            String recordNo = createNo(RECORD_NO_PREFIX);
            int updated = credentialItemMapper.markRedeemed(item.getId(), userId, recordNo);
            if (updated <= 0) {
                throw new BusinessException(ErrorCode.CREDENTIAL_UNAVAILABLE);
            }
            PointLedgerVo ledger = pointAccountService.credit(buildPointCommand(userId, points, recordNo, request.getIdempotencyKey()));
            credentialBatchMapper.increaseConsumed(batch.getId(), 1, 1);
            CredentialRedeemRecordEntity record = buildRecord(recordNo, item, batch, userId, points, request, clientIp,
                    servletRequest, ledger.getLedgerNo(), CredentialConstants.RECORD_STATUS_SUCCESS, "");
            credentialRedeemRecordMapper.insert(record);
            clearFailure(userId);
            log.info("credential redeemed, itemNo={}, userId={}, points={}, ledgerNo={}",
                    item.getItemNo(), userId, points, ledger.getLedgerNo());
            return toRedeemVo(record, item.getItemNo());
        } catch (BusinessException ex) {
            recordFailure(userId, request, clientIp, servletRequest, item, batch, 0L);
            throw ex;
        }
    }

    private void validateRedeemItem(CredentialItemEntity item, CredentialBatchEntity batch) {
        if (batch == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND);
        }
        if (!CredentialConstants.FULFILLMENT_TYPE_POINTS_REDEEM.equals(batch.getFulfillmentType())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_UNAVAILABLE);
        }
        if (!CredentialConstants.STATUS_ACTIVE.equals(batch.getStatus())
                || !CredentialConstants.STATUS_ACTIVE.equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (LocalDateTime.parse(batch.getValidFrom(), DATE_TIME_FORMATTER).isAfter(now)
                || LocalDateTime.parse(batch.getValidTo(), DATE_TIME_FORMATTER).isBefore(now)) {
            throw new BusinessException(ErrorCode.CREDENTIAL_UNAVAILABLE);
        }
    }

    private long resolvePoints(CredentialItemEntity item, CredentialBatchEntity batch) {
        String payload = StringUtils.hasText(item.getPayloadSnapshot()) ? item.getPayloadSnapshot() : batch.getPayloadConfig();
        JSONObject object = JSON.parseObject(payload);
        Long points = object.getLong("points");
        if (points == null || points <= 0L) {
            throw new BusinessException(ErrorCode.POINT_AMOUNT_INVALID);
        }
        return points;
    }

    private PointChangeCommand buildPointCommand(Long userId, long points, String recordNo, String idempotencyKey) {
        PointChangeCommand command = new PointChangeCommand();
        command.setUserId(userId);
        command.setAmount(points);
        command.setBizType(CredentialConstants.BIZ_TYPE_CREDENTIAL_REDEEM);
        command.setBizNo(recordNo);
        command.setIdempotencyKey(idempotencyKey);
        command.setOperatorType(OPERATOR_TYPE_USER);
        command.setOperatorId(String.valueOf(userId));
        command.setRemark("凭证兑换积分");
        return command;
    }

    private CredentialRedeemRecordEntity buildRecord(String recordNo,
                                                     CredentialItemEntity item,
                                                     CredentialBatchEntity batch,
                                                     Long userId,
                                                     Long points,
                                                     CredentialRedeemRequest request,
                                                     String clientIp,
                                                     HttpServletRequest servletRequest,
                                                     String ledgerNo,
                                                     String status,
                                                     String failureReason) {
        CredentialRedeemRecordEntity entity = new CredentialRedeemRecordEntity();
        entity.setRecordNo(recordNo);
        entity.setItemId(item == null ? null : item.getId());
        entity.setBatchId(batch == null ? null : batch.getId());
        entity.setCategoryId(batch == null ? null : batch.getCategoryId());
        entity.setUserId(userId);
        entity.setPoints(points == null ? 0L : points);
        entity.setIdempotencyKey(request.getIdempotencyKey());
        entity.setClientIp(clientIp);
        entity.setUserAgentHash(credentialCryptoService.sha256(servletRequest.getHeader("User-Agent")));
        entity.setDeviceFingerprint(request.getDeviceFingerprint());
        entity.setLedgerNo(ledgerNo);
        entity.setStatus(status);
        entity.setFailureReason(failureReason);
        return entity;
    }

    private void recordFailure(Long userId,
                               CredentialRedeemRequest request,
                               String clientIp,
                               HttpServletRequest servletRequest,
                               CredentialItemEntity item,
                               CredentialBatchEntity batch,
                               Long points) {
        CredentialRedeemRecordEntity record = buildRecord(createNo(RECORD_NO_PREFIX), item, batch, userId, points,
                request, clientIp, servletRequest, null, CredentialConstants.RECORD_STATUS_FAILED, FAILURE_INVALID);
        credentialRedeemRecordMapper.insert(record);
        Long current = redisCacheService.increment(REDIS_FAIL_PREFIX + userId);
        if (current != null && current == 1L) {
            redisCacheService.expire(REDIS_FAIL_PREFIX + userId, credentialProperties.getRedeemLockSeconds());
        }
        if (current != null && current >= credentialProperties.getRedeemFailureLimit()) {
            redisCacheService.set(REDIS_LOCK_PREFIX + userId, clientIp, credentialProperties.getRedeemLockSeconds());
        }
    }

    private void checkRateLimit(Long userId, String clientIp) {
        if (Boolean.TRUE.equals(redisCacheService.hasKey(REDIS_LOCK_PREFIX + userId))) {
            throw new BusinessException(ErrorCode.CREDENTIAL_REDEEM_LOCKED);
        }
        checkLimit(REDIS_LIMIT_USER_PREFIX + userId, credentialProperties.getRedeemUserLimit(),
                credentialProperties.getRedeemUserWindowSeconds());
        checkLimit(REDIS_LIMIT_IP_PREFIX + credentialCryptoService.sha256(clientIp), credentialProperties.getRedeemIpLimit(),
                credentialProperties.getRedeemIpWindowSeconds());
    }

    private void checkLimit(String key, long limit, long windowSeconds) {
        Long current = redisCacheService.increment(key);
        if (current != null && current == 1L) {
            redisCacheService.expire(key, windowSeconds);
        }
        if (current != null && current > limit) {
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    private void clearFailure(Long userId) {
        redisCacheService.delete(REDIS_FAIL_PREFIX + userId);
        redisCacheService.delete(REDIS_LOCK_PREFIX + userId);
    }

    private CredentialRedeemVo toRedeemVo(CredentialRedeemRecordEntity record, String itemNo) {
        CredentialRedeemVo vo = new CredentialRedeemVo();
        vo.setRecordNo(record.getRecordNo());
        vo.setItemNo(itemNo);
        vo.setPoints(record.getPoints());
        vo.setLedgerNo(record.getLedgerNo());
        vo.setStatus(record.getStatus());
        return vo;
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(NO_TIME_FORMATTER) + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }
}
