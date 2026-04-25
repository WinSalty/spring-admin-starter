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
import com.winsalty.quickstart.cdk.dto.CdkExportRequest;
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
import com.winsalty.quickstart.infra.outbox.TransactionOutboxService;
import com.winsalty.quickstart.infra.web.TraceIdFilter;
import com.winsalty.quickstart.points.constant.PointsConstants;
import com.winsalty.quickstart.points.entity.PointRechargeOrderEntity;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.risk.service.RiskAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private static final String EMPTY_FAILURE_REASON = "";
    private static final String OUTBOX_AGGREGATE_TYPE = "cdk_redeem";
    private static final String OUTBOX_EVENT_SUCCESS = "cdk.redeem.success";
    private static final String EXPORT_FILE_TYPE = "zip";
    private static final String EXPORT_ENCRYPTION_ALGORITHM = "AES-256-GCM";
    private static final String EXPORT_KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String EXPORT_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String EXPORT_ZIP_PAYLOAD_NAME = "cdk.enc";
    private static final String EXPORT_ZIP_MANIFEST_NAME = "manifest.json";
    private static final String EXPORT_FILE_SUFFIX = "-cdk-export.zip";
    private static final String EXPORT_SECRET_ALGORITHM = "AES";
    private static final int EXPORT_KEY_LENGTH_BITS = 256;
    private static final int EXPORT_SALT_LENGTH = 16;
    private static final int EXPORT_IV_LENGTH = 12;
    private static final int EXPORT_GCM_TAG_LENGTH_BITS = 128;
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
    private final TransactionOutboxService transactionOutboxService;
    private final RiskAlertService riskAlertService;
    private final CdkProperties cdkProperties;

    public CdkServiceImpl(CdkBatchMapper cdkBatchMapper,
                          CdkCodeMapper cdkCodeMapper,
                          CdkRedeemRecordMapper cdkRedeemRecordMapper,
                          CdkExportAuditMapper cdkExportAuditMapper,
                          PointRechargeOrderMapper pointRechargeOrderMapper,
                          BenefitGrantService benefitGrantService,
                          RedisCacheService redisCacheService,
                          TransactionOutboxService transactionOutboxService,
                          RiskAlertService riskAlertService,
                          CdkProperties cdkProperties) {
        this.cdkBatchMapper = cdkBatchMapper;
        this.cdkCodeMapper = cdkCodeMapper;
        this.cdkRedeemRecordMapper = cdkRedeemRecordMapper;
        this.cdkExportAuditMapper = cdkExportAuditMapper;
        this.pointRechargeOrderMapper = pointRechargeOrderMapper;
        this.benefitGrantService = benefitGrantService;
        this.redisCacheService = redisCacheService;
        this.transactionOutboxService = transactionOutboxService;
        this.riskAlertService = riskAlertService;
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
            // 批次数量限制放在创建阶段，避免后续审批生成大量码拖垮 Redis 和数据库。
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
        if (requiresDoubleReview(batch)) {
            if (StringUtils.hasText(batch.getApprovedBy())) {
                throw new BusinessException(ErrorCode.CDK_SECOND_APPROVAL_REQUIRED);
            }
            // 高风险批次第一次审批只记录审批人并发告警，不生成码，等待另一个管理员复核。
            cdkBatchMapper.markFirstApproved(batch.getId(), currentUsername());
            createBatchReviewAlert(batch);
            log.info("cdk batch first approved and waits second review, batchNo={}, approvedBy={}",
                    batch.getBatchNo(), currentUsername());
            return toBatchVo(cdkBatchMapper.findById(id));
        }
        List<String> plainCodes = generateCodes(batch);
        cdkBatchMapper.markApproved(batch.getId(), plainCodes.size(), currentUsername(), CdkConstants.BATCH_STATUS_ACTIVE);
        // 明文 CDK 只短期保存在 Redis 导出窗口中，数据库仅保存 HMAC hash。
        redisCacheService.set(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo(),
                String.join(EXPORT_LINE_SEPARATOR, plainCodes), cdkProperties.getExportWindowSeconds());
        log.info("cdk batch approved and generated, batchNo={}, generatedCount={}", batch.getBatchNo(), plainCodes.size());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo secondApproveBatch(Long id) {
        ensurePepperConfigured();
        CdkBatchEntity batch = loadBatchForUpdate(id);
        if (!CdkConstants.BATCH_STATUS_PENDING_APPROVAL.equals(batch.getStatus()) || !StringUtils.hasText(batch.getApprovedBy())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        if (currentUsername().equals(batch.getApprovedBy())) {
            // 二次复核必须换人，避免高价值批次由同一个管理员闭环审批。
            throw new BusinessException(ErrorCode.CDK_SECOND_APPROVER_INVALID);
        }
        if (batch.getTotalCount() > cdkProperties.getMaxBatchSize()) {
            throw new BusinessException(ErrorCode.CDK_BATCH_SIZE_EXCEEDED);
        }
        List<String> plainCodes = generateCodes(batch);
        cdkBatchMapper.markSecondApproved(batch.getId(), plainCodes.size(), currentUsername(), CdkConstants.BATCH_STATUS_ACTIVE);
        redisCacheService.set(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo(),
                String.join(EXPORT_LINE_SEPARATOR, plainCodes), cdkProperties.getExportWindowSeconds());
        log.info("cdk batch second approved and generated, batchNo={}, generatedCount={}, secondApprovedBy={}",
                batch.getBatchNo(), plainCodes.size(), currentUsername());
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
    public CdkExportVo exportBatch(Long id, CdkExportRequest request) {
        if (request == null || !StringUtils.hasText(request.getExportPassword())) {
            throw new BusinessException(ErrorCode.CDK_EXPORT_PASSWORD_REQUIRED);
        }
        CdkBatchEntity batch = loadBatchForUpdate(id);
        Object cached = redisCacheService.get(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo());
        if (!(cached instanceof String) || !StringUtils.hasText((String) cached)) {
            throw new BusinessException(ErrorCode.CDK_EXPORT_EXPIRED);
        }
        String content = (String) cached;
        // 导出是一次性操作，先删除 Redis 明文缓存，后续重复导出会失败。
        redisCacheService.delete(CdkConstants.EXPORT_CACHE_PREFIX + batch.getBatchNo());
        List<String> codes = Arrays.asList(content.split(EXPORT_LINE_SEPARATOR));
        byte[] exportPackage = buildEncryptedExportPackage(batch, content, request.getExportPassword(), codes.size());
        String fingerprint = sha256(exportPackage);
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
        vo.setFileName(batch.getBatchNo() + EXPORT_FILE_SUFFIX);
        vo.setFileType(EXPORT_FILE_TYPE);
        vo.setEncryptionAlgorithm(EXPORT_ENCRYPTION_ALGORITHM);
        vo.setEncryptedPackageBase64(Base64.getEncoder().encodeToString(exportPackage));
        log.info("cdk batch exported once, batchNo={}, count={}, fingerprint={}", batch.getBatchNo(), codes.size(), fingerprint);
        return vo;
    }

    private boolean requiresDoubleReview(CdkBatchEntity batch) {
        if (!cdkProperties.isDoubleReviewEnabled()) {
            return false;
        }
        if (CdkConstants.RISK_LEVEL_HIGH.equals(batch.getRiskLevel())
                || CdkConstants.RISK_LEVEL_CRITICAL.equals(batch.getRiskLevel())) {
            // 风险等级由创建人标记，高风险和严重风险无条件进入双人复核。
            return true;
        }
        // 普通风险批次按总积分金额判断是否需要复核。
        return calculateTotalPoints(batch) >= cdkProperties.getDoubleReviewTotalPointsThreshold();
    }

    private long calculateTotalPoints(CdkBatchEntity batch) {
        if (!CdkConstants.BENEFIT_TYPE_POINTS.equals(batch.getBenefitType())) {
            return 0L;
        }
        long points = JSON.parseObject(batch.getBenefitConfig()).getLongValue(CONFIG_POINTS);
        return points * batch.getTotalCount();
    }

    private void createBatchReviewAlert(CdkBatchEntity batch) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("batchNo", batch.getBatchNo());
        detail.put("riskLevel", batch.getRiskLevel());
        detail.put("totalCount", batch.getTotalCount());
        detail.put("totalPoints", calculateTotalPoints(batch));
        riskAlertService.createAlert(CdkConstants.ALERT_TYPE_BATCH_DOUBLE_REVIEW,
                CdkConstants.RISK_LEVEL_HIGH,
                CdkConstants.SUBJECT_TYPE_CDK_BATCH,
                batch.getBatchNo(),
                null,
                FastJsonUtils.toJsonString(detail));
    }

    private byte[] buildEncryptedExportPackage(CdkBatchEntity batch, String content, String exportPassword, int count) {
        try {
            byte[] salt = randomBytes(EXPORT_SALT_LENGTH);
            byte[] iv = randomBytes(EXPORT_IV_LENGTH);
            byte[] cipherText = encryptExportPayload(content, exportPassword, salt, iv);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
            // manifest 保存解密所需的非敏感参数；真正的 CDK 明文只存在加密 payload 中。
            writeZipEntry(zipOutputStream, EXPORT_ZIP_MANIFEST_NAME, buildExportManifest(batch, count, salt, iv).getBytes(StandardCharsets.UTF_8));
            writeZipEntry(zipOutputStream, EXPORT_ZIP_PAYLOAD_NAME, cipherText);
            zipOutputStream.close();
            return outputStream.toByteArray();
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.CDK_EXPORT_ENCRYPT_FAILED);
        } catch (java.io.IOException exception) {
            throw new BusinessException(ErrorCode.CDK_EXPORT_ENCRYPT_FAILED, "CDK 导出文件生成失败");
        }
    }

    private byte[] encryptExportPayload(String content, String exportPassword, byte[] salt, byte[] iv) throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(EXPORT_KDF_ALGORITHM);
        // 导出密码不直接作为 AES key，先通过 PBKDF2 加盐派生，提高离线爆破成本。
        KeySpec spec = new PBEKeySpec(exportPassword.toCharArray(), salt, cdkProperties.getExportEncryptIterations(), EXPORT_KEY_LENGTH_BITS);
        SecretKeySpec key = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), EXPORT_SECRET_ALGORITHM);
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(EXPORT_CIPHER_ALGORITHM);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(EXPORT_GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String buildExportManifest(CdkBatchEntity batch, int count, byte[] salt, byte[] iv) {
        Map<String, Object> manifest = new HashMap<>();
        manifest.put("batchNo", batch.getBatchNo());
        manifest.put("count", count);
        manifest.put("algorithm", EXPORT_ENCRYPTION_ALGORITHM);
        manifest.put("kdf", EXPORT_KDF_ALGORITHM);
        manifest.put("iterations", cdkProperties.getExportEncryptIterations());
        manifest.put("saltBase64", Base64.getEncoder().encodeToString(salt));
        manifest.put("ivBase64", Base64.getEncoder().encodeToString(iv));
        return FastJsonUtils.toJsonString(manifest);
    }

    private void writeZipEntry(ZipOutputStream zipOutputStream, String name, byte[] bytes) throws java.io.IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(bytes);
        zipOutputStream.closeEntry();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkRedeemResultVo redeem(CdkRedeemRequest request, HttpServletRequest servletRequest) {
        AuthUser authUser = requireCurrentUser();
        String clientIp = IpUtils.getClientIp(servletRequest);
        // 兑换前先做用户/IP/失败锁定检查，避免无效码请求继续打到数据库。
        checkRedeemRisk(authUser.getUserId(), clientIp);
        CdkRedeemRecordEntity existed = cdkRedeemRecordMapper.findByUserIdAndIdempotencyKey(authUser.getUserId(), request.getIdempotencyKey());
        if (existed != null) {
            log.info("cdk redeem idempotency hit, userId={}, redeemNo={}", authUser.getUserId(), existed.getRedeemNo());
            return buildRedeemResult(existed);
        }
        String normalizedCode = normalizeCode(request.getCdk());
        if (!isCodeFormatValid(normalizedCode)) {
            // 格式错误不查库，但计入失败次数，防止暴力猜测。
            recordRedeemFailure(authUser.getUserId());
            throw new BusinessException(ErrorCode.CDK_CODE_INVALID);
        }
        ensurePepperConfigured();
        // 数据库只保存 HMAC 后的 codeHash，兑换时用规范化后的明文计算 hash 查询。
        CdkCodeEntity code = cdkCodeMapper.findByCodeHash(hmacSha256(normalizedCode));
        CdkBatchEntity batch = code == null ? null : cdkBatchMapper.findByIdForUpdate(code.getBatchId());
        validateRedeemable(code, batch);
        String redeemNo = createNo(REDEEM_NO_PREFIX);
        CdkRedeemRecordEntity record = buildProcessingRecord(authUser, batch, code, request, servletRequest, redeemNo);
        cdkRedeemRecordMapper.insert(record);
        if (cdkCodeMapper.markRedeemed(code.getId(), authUser.getUserId(), redeemNo) == 0) {
            // markRedeemed 带状态条件，返回 0 表示并发下该码已被其他请求抢先兑换。
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
        // 权益发放服务统一处理积分入账或其他权益类型，CDK 只负责码状态和兑换记录。
        BenefitGrantResult grantResult = benefitGrantService.grant(command);
        pointRechargeOrderMapper.updateStatus(rechargeOrder.getRechargeNo(), PointsConstants.ORDER_STATUS_SUCCESS, grantResult.getSnapshot());
        cdkRedeemRecordMapper.updateResult(redeemNo, CdkConstants.RECORD_STATUS_SUCCESS,
                grantResult.getSnapshot(), "", "");
        cdkBatchMapper.incrementRedeemed(batch.getId());
        transactionOutboxService.createEvent(OUTBOX_AGGREGATE_TYPE, redeemNo, OUTBOX_EVENT_SUCCESS,
                buildRedeemOutboxPayload(authUser.getUserId(), batch, redeemNo, grantResult));
        // 兑换成功清理失败计数和锁定状态，避免用户后续正常兑换受历史失败影响。
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

    private String buildRedeemOutboxPayload(Long userId,
                                            CdkBatchEntity batch,
                                            String redeemNo,
                                            BenefitGrantResult grantResult) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("redeemNo", redeemNo);
        payload.put("batchNo", batch.getBatchNo());
        payload.put("benefitType", batch.getBenefitType());
        payload.put("grantedPoints", grantResult.getGrantedPoints());
        return FastJsonUtils.toJsonString(payload);
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
            // 明文码不落库，只保存 HMAC，导出窗口结束后后台也无法恢复明文。
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
        // 批次有效期按服务端当前时间判断，客户端时间不参与兑换有效性校验。
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
        record.setFailureCode(EMPTY_FAILURE_REASON);
        record.setFailureMessage(EMPTY_FAILURE_REASON);
        record.setClientIp(IpUtils.getClientIp(servletRequest));
        // UA 只保存 hash，满足风控关联分析，同时避免记录完整浏览器指纹。
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
        order.setChannel(PointsConstants.CHANNEL_CDK);
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
        // 用户和 IP 双维度限流，既限制单用户刷码，也限制同 IP 批量猜码。
        checkLimit(CdkConstants.REDEEM_USER_LIMIT_PREFIX + digestKey(userKey), cdkProperties.getRedeemUserLimit(),
                cdkProperties.getRedeemUserWindowSeconds());
        checkLimit(CdkConstants.REDEEM_IP_LIMIT_PREFIX + digestKey(defaultText(clientIp)), cdkProperties.getRedeemIpLimit(),
                cdkProperties.getRedeemIpWindowSeconds());
    }

    private void checkLimit(String key, long limit, long windowSeconds) {
        Long current = redisCacheService.increment(key);
        if (current != null && current == 1L) {
            // 限流计数器第一次创建时绑定窗口 TTL，后续窗口到期自动归零。
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
            // 失败计数的 TTL 与锁定时长一致，低频失败不会永久累计。
            redisCacheService.expire(failKey, cdkProperties.getRedeemLockSeconds());
        }
        if (current != null && current >= cdkProperties.getRedeemFailureLimit()) {
            redisCacheService.set(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)), "locked",
                    cdkProperties.getRedeemLockSeconds());
            redisCacheService.delete(failKey);
            createRedeemLockedAlert(userId);
            log.info("cdk redeem locked after repeated failures, userId={}, lockSeconds={}", userId, cdkProperties.getRedeemLockSeconds());
        }
    }

    private void createRedeemLockedAlert(Long userId) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("userId", userId);
        detail.put("failureLimit", cdkProperties.getRedeemFailureLimit());
        detail.put("lockSeconds", cdkProperties.getRedeemLockSeconds());
        riskAlertService.createAlert(CdkConstants.ALERT_TYPE_REDEEM_LOCKED,
                CdkConstants.RISK_LEVEL_HIGH,
                CdkConstants.SUBJECT_TYPE_USER,
                String.valueOf(userId),
                userId,
                FastJsonUtils.toJsonString(detail));
    }

    private void recordRedeemSuccess(Long userId) {
        redisCacheService.delete(CdkConstants.REDEEM_FAIL_PREFIX + digestKey(String.valueOf(userId)));
        redisCacheService.delete(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)));
    }

    private String normalizeCode(String code) {
        // 支持用户复制时带空格或小写，统一规范化后再做校验和 hash。
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
        // 校验位基于码体和固定盐计算，用于在查库前快速过滤明显伪造的 CDK。
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
        // 校验位追加在末尾，兑换时可先验格式再查 HMAC，减少数据库压力。
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

    private String sha256(byte[] value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256_ALGORITHM).digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk sha256 failed", exception);
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
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
        vo.setSecondApprovedBy(entity.getSecondApprovedBy());
        vo.setSecondApprovedAt(entity.getSecondApprovedAt());
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
