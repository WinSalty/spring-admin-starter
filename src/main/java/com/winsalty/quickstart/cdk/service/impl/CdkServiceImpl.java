package com.winsalty.quickstart.cdk.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.benefit.dto.BenefitGrantCommand;
import com.winsalty.quickstart.benefit.dto.BenefitGrantResult;
import com.winsalty.quickstart.benefit.service.BenefitGrantService;
import com.winsalty.quickstart.cdk.config.CdkProperties;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkBatchListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRequest;
import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import com.winsalty.quickstart.cdk.entity.CdkExportAuditEntity;
import com.winsalty.quickstart.cdk.entity.CdkRedeemRecordEntity;
import com.winsalty.quickstart.cdk.mapper.CdkBatchMapper;
import com.winsalty.quickstart.cdk.mapper.CdkCodeMapper;
import com.winsalty.quickstart.cdk.mapper.CdkExportAuditMapper;
import com.winsalty.quickstart.cdk.mapper.CdkRedeemRecordMapper;
import com.winsalty.quickstart.cdk.service.CdkService;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkExportVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkRedeemResultVo;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import com.winsalty.quickstart.infra.web.TraceIdFilter;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * CDK 服务实现。
 * 实现批次创建审批、HMAC 存储、一次性导出缓存、兑换限流和积分权益发放。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class CdkServiceImpl extends BaseService implements CdkService {

    private static final Logger log = LoggerFactory.getLogger(CdkServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String BATCH_NO_PREFIX = "CB";
    private static final String REDEEM_NO_PREFIX = "CR";
    private static final String RECHARGE_NO_PREFIX = "PR";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String CODE_SEPARATOR = "-";
    private static final String EXPORT_LINE_SEPARATOR = "\n";
    private static final String CONFIG_POINTS = "points";
    private static final String CONFIG_GRANT_REASON = "grantReason";
    private static final String CONFIG_REMARK = "remark";
    private static final String GRANT_REASON_CDK = "cdk_reward";
    private static final String UA_HEADER = "User-Agent";
    private static final String UNKNOWN_TARGET = "unknown";
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int PEPPER_MIN_LENGTH = 32;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final CdkBatchMapper cdkBatchMapper;
    private final CdkCodeMapper cdkCodeMapper;
    private final CdkRedeemRecordMapper cdkRedeemRecordMapper;
    private final CdkExportAuditMapper cdkExportAuditMapper;
    private final PointRechargeOrderMapper pointRechargeOrderMapper;
    private final BenefitGrantService benefitGrantService;
    private final RedisCacheService redisCacheService;
    private final CdkProperties cdkProperties;

    public CdkServiceImpl(CdkBatchMapper cdkBatchMapper,
                          CdkCodeMapper cdkCodeMapper,
                          CdkRedeemRecordMapper cdkRedeemRecordMapper,
                          CdkExportAuditMapper cdkExportAuditMapper,
                          PointRechargeOrderMapper pointRechargeOrderMapper,
                          BenefitGrantService benefitGrantService,
                          RedisCacheService redisCacheService,
                          CdkProperties cdkProperties) {
        this.cdkBatchMapper = cdkBatchMapper;
        this.cdkCodeMapper = cdkCodeMapper;
        this.cdkRedeemRecordMapper = cdkRedeemRecordMapper;
        this.cdkExportAuditMapper = cdkExportAuditMapper;
        this.pointRechargeOrderMapper = pointRechargeOrderMapper;
        this.benefitGrantService = benefitGrantService;
        this.redisCacheService = redisCacheService;
        this.cdkProperties = cdkProperties;
    }

    @Override
    public PageResponse<CdkBatchVo> listBatches(CdkBatchListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkBatchEntity> entities = cdkBatchMapper.findPage(request.getKeyword(), request.getStatus(),
                request.getBenefitType(), offset(pageNo, pageSize), pageSize);
        long total = cdkBatchMapper.countPage(request.getKeyword(), request.getStatus(), request.getBenefitType());
        return new PageResponse<CdkBatchVo>(toBatchVoList(entities), pageNo, pageSize, total);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo createBatch(CdkBatchCreateRequest request) {
        if (!CdkConstants.BENEFIT_TYPE_POINTS.equals(request.getBenefitType())) {
            throw new BusinessException(ErrorCode.CDK_BENEFIT_UNSUPPORTED);
        }
        if (request.getTotalCount() > cdkProperties.getMaxBatchSize()) {
            throw new BusinessException(ErrorCode.CDK_BATCH_SIZE_EXCEEDED);
        }
        CdkBatchEntity entity = new CdkBatchEntity();
        entity.setBatchNo(createNo(BATCH_NO_PREFIX));
        entity.setBatchName(request.getBatchName());
        entity.setBenefitType(request.getBenefitType());
        entity.setBenefitConfig(buildBenefitConfig(request));
        entity.setTotalCount(request.getTotalCount());
        entity.setGeneratedCount(0);
        entity.setRedeemedCount(0);
        entity.setValidFrom(request.getValidFrom());
        entity.setValidTo(request.getValidTo());
        entity.setStatus(CdkConstants.BATCH_STATUS_DRAFT);
        entity.setRiskLevel(StringUtils.hasText(request.getRiskLevel()) ? request.getRiskLevel() : CdkConstants.RISK_LEVEL_NORMAL);
        entity.setCreatedBy(currentUsername());
        entity.setExportCount(0);
        cdkBatchMapper.insert(entity);
        log.info("cdk batch created, batchNo={}, totalCount={}, benefitType={}",
                entity.getBatchNo(), entity.getTotalCount(), entity.getBenefitType());
        return toBatchVo(cdkBatchMapper.findById(entity.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo submitBatch(Long id) {
        CdkBatchEntity batch = loadBatchForUpdate(id);
        if (!CdkConstants.BATCH_STATUS_DRAFT.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        cdkBatchMapper.updateStatus(id, CdkConstants.BATCH_STATUS_PENDING_APPROVAL);
        log.info("cdk batch submitted, batchNo={}", batch.getBatchNo());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo approveBatch(Long id) {
        ensurePepperConfigured();
        CdkBatchEntity batch = loadBatchForUpdate(id);
        if (!CdkConstants.BATCH_STATUS_PENDING_APPROVAL.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        if (batch.getTotalCount() > cdkProperties.getMaxBatchSize()) {
            throw new BusinessException(ErrorCode.CDK_BATCH_SIZE_EXCEEDED);
        }
        List<String> plainCodes = generateCodes(batch);
        cdkBatchMapper.markApproved(batch.getId(), plainCodes.size(), currentUsername());
        redisCacheService.set(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo(),
                String.join(EXPORT_LINE_SEPARATOR, plainCodes), cdkProperties.getExportWindowSeconds());
        log.info("cdk batch approved and generated, batchNo={}, generatedCount={}", batch.getBatchNo(), plainCodes.size());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo pauseBatch(Long id) {
        CdkBatchEntity batch = loadBatchForUpdate(id);
        if (!CdkConstants.BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        cdkBatchMapper.updateStatus(id, CdkConstants.BATCH_STATUS_PAUSED);
        log.info("cdk batch paused, batchNo={}", batch.getBatchNo());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo voidBatch(Long id) {
        CdkBatchEntity batch = loadBatchForUpdate(id);
        if (CdkConstants.BATCH_STATUS_VOIDED.equals(batch.getStatus())) {
            return toBatchVo(batch);
        }
        cdkBatchMapper.updateStatus(id, CdkConstants.BATCH_STATUS_VOIDED);
        log.info("cdk batch voided, batchNo={}", batch.getBatchNo());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkExportVo exportBatch(Long id) {
        CdkBatchEntity batch = loadBatchForUpdate(id);
        Object cached = redisCacheService.get(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo());
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.CDK_EXPORT_EXPIRED);
        }
        String content = (String) cached;
        redisCacheService.delete(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo());
        String fingerprint = sha256(content);
        List<String> codes = Arrays.asList(content.split(EXPORT_LINE_SEPARATOR));
        CdkExportAuditEntity audit = new CdkExportAuditEntity();
        audit.setBatchId(batch.getId());
        audit.setBatchNo(batch.getBatchNo());
        audit.setExportedBy(currentUsername());
        audit.setExportCount(codes.size());
        audit.setFileFingerprint(fingerprint);
        cdkExportAuditMapper.insert(audit);
        cdkBatchMapper.incrementExport(batch.getId());
        CdkExportVo vo = new CdkExportVo();
        vo.setBatchNo(batch.getBatchNo());
        vo.setCount(codes.size());
        vo.setFingerprint(fingerprint);
        vo.setCodes(codes);
        log.info("cdk batch exported once, batchNo={}, count={}, fingerprint={}", batch.getBatchNo(), codes.size(), fingerprint);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkRedeemResultVo redeem(CdkRedeemRequest request, HttpServletRequest servletRequest) {
        AuthUser authUser = requireCurrentUser();
        String clientIp = IpUtils.getClientIp(servletRequest);
        checkRedeemRisk(authUser.getUserId(), clientIp);
        CdkRedeemRecordEntity existed = cdkRedeemRecordMapper.findByUserIdAndIdempotencyKey(authUser.getUserId(), request.getIdempotencyKey());
        if (existed != null) {
            log.info("cdk redeem idempotency hit, userId={}, redeemNo={}", authUser.getUserId(), existed.getRedeemNo());
            return buildRedeemResult(existed);
        }
        String normalizedCode = normalizeCode(request.getCdk());
        if (!isCodeFormatValid(normalizedCode)) {
            recordRedeemFailure(authUser.getUserId());
            throw new BusinessException(ErrorCode.CDK_CODE_INVALID);
        }
        ensurePepperConfigured();
        CdkCodeEntity code = cdkCodeMapper.findByCodeHash(hmacSha256(normalizedCode));
        CdkBatchEntity batch = code == null ? null : cdkBatchMapper.findByIdForUpdate(code.getBatchId());
        validateRedeemable(code, batch);
        String redeemNo = createNo(REDEEM_NO_PREFIX);
        CdkRedeemRecordEntity record = buildProcessingRecord(authUser, batch, code, request, servletRequest, redeemNo);
        cdkRedeemRecordMapper.insert(record);
        if (cdkCodeMapper.markRedeemed(code.getId(), authUser.getUserId(), redeemNo) == 0) {
            recordRedeemFailure(authUser.getUserId());
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        PointRechargeOrderEntity rechargeOrder = buildRechargeOrder(authUser, batch, redeemNo, request.getIdempotencyKey());
        pointRechargeOrderMapper.insert(rechargeOrder);
        BenefitGrantCommand command = new BenefitGrantCommand();
        command.setUserId(authUser.getUserId());
        command.setBenefitType(batch.getBenefitType());
        command.setBenefitConfig(batch.getBenefitConfig());
        command.setBizNo(rechargeOrder.getRechargeNo());
        command.setIdempotencyKey("cdk:" + redeemNo);
        command.setOperatorType(PointsConstants.OPERATOR_TYPE_USER);
        command.setOperatorId(String.valueOf(authUser.getUserId()));
        command.setRemark("CDK 兑换积分");
        BenefitGrantResult grantResult = benefitGrantService.grant(command);
        pointRechargeOrderMapper.updateStatus(rechargeOrder.getRechargeNo(), PointsConstants.ORDER_STATUS_SUCCESS, grantResult.getSnapshot());
        cdkRedeemRecordMapper.updateResult(redeemNo, CdkConstants.RECORD_STATUS_SUCCESS,
                grantResult.getSnapshot(), "", "");
        cdkBatchMapper.incrementRedeemed(batch.getId());
        recordRedeemSuccess(authUser.getUserId());
        log.info("cdk redeemed, userId={}, redeemNo={}, batchNo={}, benefitType={}",
                authUser.getUserId(), redeemNo, batch.getBatchNo(), batch.getBenefitType());
        CdkRedeemResultVo result = new CdkRedeemResultVo();
        result.setRedeemNo(redeemNo);
        result.setBenefitType(batch.getBenefitType());
        result.setGrantedPoints(grantResult.getGrantedPoints());
        result.setAvailablePoints(grantResult.getAvailablePoints());
        result.setFrozenPoints(grantResult.getFrozenPoints());
        result.setStatus(CdkConstants.RECORD_STATUS_SUCCESS);
        return result;
    }

    @Override
    public PageResponse<CdkRedeemRecordVo> listRedeemRecords(CdkRedeemRecordListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkRedeemRecordEntity> entities = cdkRedeemRecordMapper.findPage(request.getUserId(), request.getBatchId(),
                request.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = cdkRedeemRecordMapper.countPage(request.getUserId(), request.getBatchId(), request.getStatus());
        return new PageResponse<CdkRedeemRecordVo>(toRedeemRecordVoList(entities), pageNo, pageSize, total);
    }

    private List<String> generateCodes(CdkBatchEntity batch) {
        List<String> plainCodes = new ArrayList<String>();
        String yearMonth = LocalDate.now().format(YEAR_MONTH_FORMATTER);
        for (int index = 0; index < batch.getTotalCount(); index++) {
            String plainCode = generatePlainCode(yearMonth);
            CdkCodeEntity code = new CdkCodeEntity();
            code.setBatchId(batch.getId());
            code.setCodeHash(hmacSha256(plainCode));
            code.setCodePrefix(CdkConstants.CODE_PREFIX + CODE_SEPARATOR + yearMonth);
            code.setChecksum(resolveChecksum(plainCode));
            code.setStatus(CdkConstants.CODE_STATUS_ACTIVE);
            code.setVersion(0L);
            cdkCodeMapper.insert(code);
            plainCodes.add(plainCode);
        }
        return plainCodes;
    }

    private String buildBenefitConfig(CdkBatchCreateRequest request) {
        JSONObject config = new JSONObject();
        config.put(CONFIG_POINTS, request.getPoints());
        config.put(CONFIG_GRANT_REASON, GRANT_REASON_CDK);
        config.put(CONFIG_REMARK, request.getRemark());
        return config.toJSONString();
    }

    private void validateRedeemable(CdkCodeEntity code, CdkBatchEntity batch) {
        if (code == null || batch == null) {
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        if (!CdkConstants.CODE_STATUS_ACTIVE.equals(code.getStatus()) || !CdkConstants.BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validFrom = LocalDateTime.parse(batch.getValidFrom(), DATE_TIME_FORMATTER);
        LocalDateTime validTo = LocalDateTime.parse(batch.getValidTo(), DATE_TIME_FORMATTER);
        if (now.isBefore(validFrom) || now.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
    }

    private CdkRedeemRecordEntity buildProcessingRecord(AuthUser authUser,
                                                        CdkBatchEntity batch,
                                                        CdkCodeEntity code,
                                                        CdkRedeemRequest request,
                                                        HttpServletRequest servletRequest,
                                                        String redeemNo) {
        CdkRedeemRecordEntity record = new CdkRedeemRecordEntity();
        record.setRedeemNo(redeemNo);
        record.setUserId(authUser.getUserId());
        record.setBatchId(batch.getId());
        record.setCodeId(code.getId());
        record.setBenefitType(batch.getBenefitType());
        record.setBenefitSnapshot(batch.getBenefitConfig());
        record.setStatus(CdkConstants.RECORD_STATUS_PROCESSING);
        record.setClientIp(IpUtils.getClientIp(servletRequest));
        record.setUserAgentHash(sha256(servletRequest == null ? "" : servletRequest.getHeader(UA_HEADER)));
        record.setTraceId(currentTraceId());
        record.setIdempotencyKey(request.getIdempotencyKey());
        return record;
    }

    private PointRechargeOrderEntity buildRechargeOrder(AuthUser authUser,
                                                        CdkBatchEntity batch,
                                                        String redeemNo,
                                                        String idempotencyKey) {
        PointRechargeOrderEntity order = new PointRechargeOrderEntity();
        order.setRechargeNo(createNo(RECHARGE_NO_PREFIX));
        order.setUserId(authUser.getUserId());
        order.setChannel("cdk");
        order.setAmount(JSON.parseObject(batch.getBenefitConfig()).getLongValue(CONFIG_POINTS));
        order.setStatus(PointsConstants.ORDER_STATUS_PROCESSING);
        order.setExternalNo(redeemNo);
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestSnapshot("{\"source\":\"cdk\"}");
        order.setResultSnapshot("{}");
        return order;
    }

    private CdkBatchEntity loadBatchForUpdate(Long id) {
        CdkBatchEntity batch = cdkBatchMapper.findByIdForUpdate(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.CDK_BATCH_NOT_FOUND);
        }
        return batch;
    }

    private void checkRedeemRisk(Long userId, String clientIp) {
        String userKey = String.valueOf(userId);
        if (Boolean.TRUE.equals(redisCacheService.hasKey(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(userKey)))) {
            throw new BusinessException(ErrorCode.CDK_REDEEM_LOCKED);
        }
        checkLimit(CdkConstants.REDEEM_USER_LIMIT_PREFIX + digestKey(userKey), cdkProperties.getRedeemUserLimit(),
                cdkProperties.getRedeemUserWindowSeconds());
        checkLimit(CdkConstants.REDEEM_IP_LIMIT_PREFIX + digestKey(defaultText(clientIp)), cdkProperties.getRedeemIpLimit(),
                cdkProperties.getRedeemIpWindowSeconds());
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

    private void recordRedeemFailure(Long userId) {
        String failKey = CdkConstants.REDEEM_FAIL_PREFIX + digestKey(String.valueOf(userId));
        Long current = redisCacheService.increment(failKey);
        if (current != null && current == 1L) {
            redisCacheService.expire(failKey, cdkProperties.getRedeemLockSeconds());
        }
        if (current != null && current >= cdkProperties.getRedeemFailureLimit()) {
            redisCacheService.set(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)), "locked",
                    cdkProperties.getRedeemLockSeconds());
            redisCacheService.delete(failKey);
            log.info("cdk redeem locked after repeated failures, userId={}, lockSeconds={}", userId, cdkProperties.getRedeemLockSeconds());
        }
    }

    private void recordRedeemSuccess(Long userId) {
        redisCacheService.delete(CdkConstants.REDEEM_FAIL_PREFIX + digestKey(String.valueOf(userId)));
        redisCacheService.delete(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)));
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().replace(" ", "").toUpperCase();
    }

    private boolean isCodeFormatValid(String code) {
        String[] parts = code.split(CODE_SEPARATOR);
        if (parts.length < CdkConstants.MIN_CODE_PARTS) {
            return false;
        }
        if (!CdkConstants.CODE_PREFIX.equals(parts[0]) || parts[1].length() != CdkConstants.YEAR_MONTH_LENGTH) {
            return false;
        }
        String checksum = parts[parts.length - 1];
        if (checksum.length() != CdkConstants.CHECKSUM_LENGTH) {
            return false;
        }
        for (int index = 2; index < parts.length - 1; index++) {
            if (parts[index].length() != CdkConstants.CODE_GROUP_LENGTH) {
                return false;
            }
        }
        return checksum.equals(calculateChecksum(resolveCodeBody(code)));
    }

    private String generatePlainCode(String yearMonth) {
        byte[] randomBytes = new byte[CdkConstants.RANDOM_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String randomPart = toHex(randomBytes).toUpperCase();
        StringBuilder builder = new StringBuilder();
        builder.append(CdkConstants.CODE_PREFIX).append(CODE_SEPARATOR).append(yearMonth);
        for (int index = 0; index < randomPart.length(); index += CdkConstants.CODE_GROUP_LENGTH) {
            builder.append(CODE_SEPARATOR)
                    .append(randomPart, index, index + CdkConstants.CODE_GROUP_LENGTH);
        }
        String body = builder.toString();
        return body + CODE_SEPARATOR + calculateChecksum(body);
    }

    private String resolveCodeBody(String code) {
        int lastSeparatorIndex = code.lastIndexOf(CODE_SEPARATOR);
        return lastSeparatorIndex <= 0 ? code : code.substring(0, lastSeparatorIndex);
    }

    private String resolveChecksum(String code) {
        int lastSeparatorIndex = code.lastIndexOf(CODE_SEPARATOR);
        return lastSeparatorIndex <= 0 ? "" : code.substring(lastSeparatorIndex + 1);
    }

    private String calculateChecksum(String body) {
        return sha256(body + CdkConstants.CHECKSUM_SALT).substring(0, CdkConstants.CHECKSUM_LENGTH).toUpperCase();
    }

    private void ensurePepperConfigured() {
        if (cdkProperties.getPepper() == null || cdkProperties.getPepper().getBytes(StandardCharsets.UTF_8).length < PEPPER_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.CDK_PEPPER_MISSING);
        }
    }

    private String hmacSha256(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(cdkProperties.getPepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("cdk hmac failed", exception);
        }
    }

    private String digestKey(String value) {
        return sha256(defaultText(value));
    }

    private String sha256(String value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256_ALGORITHM).digest(defaultText(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk sha256 failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            chars[index * 2] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            chars[index * 2 + 1] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(chars);
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return StringUtils.hasText(traceId) ? traceId : "";
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN_TARGET;
    }

    private int pageNo(Integer value) {
        return value == null || value < CdkConstants.DEFAULT_PAGE_NO ? CdkConstants.DEFAULT_PAGE_NO : value;
    }

    private int pageSize(Integer value) {
        if (value == null || value < CdkConstants.DEFAULT_PAGE_NO) {
            return CdkConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, CdkConstants.MAX_PAGE_SIZE);
    }

    private int offset(int pageNo, int pageSize) {
        return (pageNo - CdkConstants.DEFAULT_PAGE_NO) * pageSize;
    }

    private CdkRedeemResultVo buildRedeemResult(CdkRedeemRecordEntity entity) {
        CdkRedeemResultVo vo = new CdkRedeemResultVo();
        vo.setRedeemNo(entity.getRedeemNo());
        vo.setBenefitType(entity.getBenefitType());
        vo.setStatus(entity.getStatus());
        if (StringUtils.hasText(entity.getBenefitSnapshot())) {
            JSONObject snapshot = JSON.parseObject(entity.getBenefitSnapshot());
            vo.setGrantedPoints(snapshot.getLong("grantedPoints"));
            vo.setAvailablePoints(snapshot.getLong("availablePoints"));
            vo.setFrozenPoints(snapshot.getLong("frozenPoints"));
        }
        return vo;
    }

    private CdkBatchVo toBatchVo(CdkBatchEntity entity) {
        CdkBatchVo vo = new CdkBatchVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setBatchNo(entity.getBatchNo());
        vo.setBatchName(entity.getBatchName());
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitConfig(entity.getBenefitConfig());
        vo.setTotalCount(entity.getTotalCount());
        vo.setGeneratedCount(entity.getGeneratedCount());
        vo.setRedeemedCount(entity.getRedeemedCount());
        vo.setValidFrom(entity.getValidFrom());
        vo.setValidTo(entity.getValidTo());
        vo.setStatus(entity.getStatus());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setApprovedBy(entity.getApprovedBy());
        vo.setApprovedAt(entity.getApprovedAt());
        vo.setExportCount(entity.getExportCount());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<CdkBatchVo> toBatchVoList(List<CdkBatchEntity> entities) {
        List<CdkBatchVo> records = new ArrayList<CdkBatchVo>();
        for (CdkBatchEntity entity : entities) {
            records.add(toBatchVo(entity));
        }
        return records;
    }

    private CdkRedeemRecordVo toRedeemRecordVo(CdkRedeemRecordEntity entity) {
        CdkRedeemRecordVo vo = new CdkRedeemRecordVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setRedeemNo(entity.getRedeemNo());
        vo.setUserId(String.valueOf(entity.getUserId()));
        vo.setBatchId(entity.getBatchId() == null ? "" : String.valueOf(entity.getBatchId()));
        vo.setCodeId(entity.getCodeId() == null ? "" : String.valueOf(entity.getCodeId()));
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitSnapshot(entity.getBenefitSnapshot());
        vo.setStatus(entity.getStatus());
        vo.setFailureCode(entity.getFailureCode());
        vo.setFailureMessage(entity.getFailureMessage());
        vo.setClientIp(entity.getClientIp());
        vo.setTraceId(entity.getTraceId());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<CdkRedeemRecordVo> toRedeemRecordVoList(List<CdkRedeemRecordEntity> entities) {
        List<CdkRedeemRecordVo> records = new ArrayList<CdkRedeemRecordVo>();
        for (CdkRedeemRecordEntity entity : entities) {
            records.add(toRedeemRecordVo(entity));
        }
        return records;
    }
}
