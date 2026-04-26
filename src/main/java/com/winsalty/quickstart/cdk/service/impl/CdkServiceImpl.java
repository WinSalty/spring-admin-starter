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
import com.winsalty.quickstart.cdk.dto.CdkCodeListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeStatusRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkRedeemRequest;
import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import com.winsalty.quickstart.cdk.entity.CdkRedeemRecordEntity;
import com.winsalty.quickstart.cdk.mapper.CdkBatchMapper;
import com.winsalty.quickstart.cdk.mapper.CdkCodeMapper;
import com.winsalty.quickstart.cdk.mapper.CdkRedeemRecordMapper;
import com.winsalty.quickstart.cdk.service.CdkService;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkCodeVo;
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

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CDK 服务实现。
 * 实现管理员批次生成、HMAC 校验、加密明文查看、兑换限流和积分权益发放。
 * 创建日期：2026-04-24
 * author：sunshengxian
 */
@Service
public class CdkServiceImpl extends BaseService implements CdkService {

    private static final Logger log = LoggerFactory.getLogger(CdkServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BATCH_NO_PREFIX = "CB";
    private static final String REDEEM_NO_PREFIX = "CR";
    private static final String RECHARGE_NO_PREFIX = "PR";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String CODE_SEPARATOR = "-";
    private static final String CONFIG_POINTS = "points";
    private static final String CONFIG_GRANT_REASON = "grantReason";
    private static final String CONFIG_REMARK = "remark";
    private static final String GRANT_REASON_CDK = "cdk_reward";
    private static final String UA_HEADER = "User-Agent";
    private static final String UNKNOWN_TARGET = "unknown";
    private static final String EMPTY_FAILURE_REASON = "";
    private static final String EMPTY_CODE_PREFIX = "";
    private static final String OUTBOX_AGGREGATE_TYPE = "cdk_redeem";
    private static final String OUTBOX_EVENT_SUCCESS = "cdk.redeem.success";
    private static final String CODE_ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final String CODE_SECRET_ALGORITHM = "AES";
    private static final String CODE_ENCRYPTION_CONTEXT = ":cdk-code:v1";
    private static final int CODE_IV_LENGTH = 12;
    private static final int CODE_GCM_TAG_LENGTH_BITS = 128;
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int PEPPER_MIN_LENGTH = 32;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    /**
     * 批量生成 CDK 时的进度日志间隔，避免大批次任务长时间无可观测输出。
     */
    private static final int CODE_GENERATE_PROGRESS_LOG_INTERVAL = 1000;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final CdkBatchMapper cdkBatchMapper;
    private final CdkCodeMapper cdkCodeMapper;
    private final CdkRedeemRecordMapper cdkRedeemRecordMapper;
    private final PointRechargeOrderMapper pointRechargeOrderMapper;
    private final BenefitGrantService benefitGrantService;
    private final RedisCacheService redisCacheService;
    private final TransactionOutboxService transactionOutboxService;
    private final RiskAlertService riskAlertService;
    private final CdkProperties cdkProperties;

    /**
     * 构造 CDK 服务依赖。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    public CdkServiceImpl(CdkBatchMapper cdkBatchMapper,
                          CdkCodeMapper cdkCodeMapper,
                          CdkRedeemRecordMapper cdkRedeemRecordMapper,
                          PointRechargeOrderMapper pointRechargeOrderMapper,
                          BenefitGrantService benefitGrantService,
                          RedisCacheService redisCacheService,
                          TransactionOutboxService transactionOutboxService,
                          RiskAlertService riskAlertService,
                          CdkProperties cdkProperties) {
        this.cdkBatchMapper = cdkBatchMapper;
        this.cdkCodeMapper = cdkCodeMapper;
        this.cdkRedeemRecordMapper = cdkRedeemRecordMapper;
        this.pointRechargeOrderMapper = pointRechargeOrderMapper;
        this.benefitGrantService = benefitGrantService;
        this.redisCacheService = redisCacheService;
        this.transactionOutboxService = transactionOutboxService;
        this.riskAlertService = riskAlertService;
        this.cdkProperties = cdkProperties;
    }

    /**
     * 分页查询 CDK 批次。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    @Override
    public PageResponse<CdkBatchVo> listBatches(CdkBatchListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkBatchEntity> entities = cdkBatchMapper.findPage(request.getKeyword(), request.getStatus(),
                request.getBenefitType(), offset(pageNo, pageSize), pageSize);
        long total = cdkBatchMapper.countPage(request.getKeyword(), request.getStatus(), request.getBenefitType());
        return new PageResponse<CdkBatchVo>(toBatchVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 创建 CDK 批次并立即生成可兑换码。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo createBatch(CdkBatchCreateRequest request) {
        ensurePepperConfigured();
        if (!CdkConstants.BENEFIT_TYPE_POINTS.equals(request.getBenefitType())) {
            log.error("cdk batch generation rejected, reason=unsupported_benefit_type, benefitType={}, operator={}",
                    request.getBenefitType(), currentUsername());
            throw new BusinessException(ErrorCode.CDK_BENEFIT_UNSUPPORTED);
        }
        if (request.getTotalCount() > cdkProperties.getMaxBatchSize()) {
            // 批次数量限制放在生成入口，避免单次事务写入过大。
            log.error("cdk batch generation rejected, reason=batch_size_exceeded, requestedCount={}, maxBatchSize={}, operator={}",
                    request.getTotalCount(), cdkProperties.getMaxBatchSize(), currentUsername());
            throw new BusinessException(ErrorCode.CDK_BATCH_SIZE_EXCEEDED);
        }
        log.info("cdk batch generation started, batchName={}, totalCount={}, benefitType={}, randomBytes={}, operator={}",
                request.getBatchName(), request.getTotalCount(), request.getBenefitType(), CdkConstants.RANDOM_BYTE_LENGTH, currentUsername());
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
        entity.setStatus(CdkConstants.BATCH_STATUS_ACTIVE);
        entity.setRiskLevel(StringUtils.hasText(request.getRiskLevel()) ? request.getRiskLevel() : CdkConstants.RISK_LEVEL_NORMAL);
        entity.setCreatedBy(currentUsername());
        entity.setExportCount(0);
        cdkBatchMapper.insert(entity);
        log.info("cdk batch record created, batchNo={}, batchId={}, totalCount={}, operator={}",
                entity.getBatchNo(), entity.getId(), entity.getTotalCount(), currentUsername());
        List<String> plainCodes = generateCodes(entity);
        cdkBatchMapper.markGenerated(entity.getId(), plainCodes.size(), CdkConstants.BATCH_STATUS_ACTIVE);
        log.info("cdk batch generated, batchNo={}, totalCount={}, benefitType={}, operator={}",
                entity.getBatchNo(), entity.getTotalCount(), entity.getBenefitType(), currentUsername());
        return toBatchVo(cdkBatchMapper.findById(entity.getId()));
    }

    /**
     * 整批作废 CDK，并同步失效所有未兑换码。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchVo voidBatch(Long id) {
        CdkBatchEntity batch = loadBatchForUpdate(id);
        log.info("cdk batch void started, batchNo={}, batchId={}, status={}, operator={}",
                batch.getBatchNo(), batch.getId(), batch.getStatus(), currentUsername());
        int disabledCount = cdkCodeMapper.disableActiveByBatchId(id);
        if (!CdkConstants.BATCH_STATUS_VOIDED.equals(batch.getStatus())) {
            cdkBatchMapper.updateStatus(id, CdkConstants.BATCH_STATUS_VOIDED);
        }
        log.info("cdk batch voided, batchNo={}, disabledCount={}, operator={}",
                batch.getBatchNo(), disabledCount, currentUsername());
        return toBatchVo(cdkBatchMapper.findById(id));
    }

    /**
     * 用户兑换 CDK，包含格式校验、风控、原子状态更新和权益发放。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkRedeemResultVo redeem(CdkRedeemRequest request, HttpServletRequest servletRequest) {
        AuthUser authUser = requireCurrentUser();
        String clientIp = IpUtils.getClientIp(servletRequest);
        String clientIpDigest = digestKey(defaultText(clientIp));
        log.info("cdk redeem started, userId={}, clientIpDigest={}, idempotencyKey={}",
                authUser.getUserId(), clientIpDigest, request.getIdempotencyKey());
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
            log.info("cdk redeem rejected before lookup, reason=invalid_format, userId={}, codeLength={}, partCount={}",
                    authUser.getUserId(), normalizedCode.length(), normalizedCode.split(CODE_SEPARATOR).length);
            recordRedeemFailure(authUser.getUserId());
            throw new BusinessException(ErrorCode.CDK_CODE_INVALID);
        }
        ensurePepperConfigured();
        // 数据库只保存 HMAC 后的 codeHash，兑换时用规范化后的明文计算 hash 查询。
        CdkCodeEntity code = cdkCodeMapper.findByCodeHash(hmacSha256(normalizedCode));
        if (code == null) {
            log.info("cdk redeem rejected after lookup, reason=code_not_found, userId={}", authUser.getUserId());
        }
        CdkBatchEntity batch = code == null ? null : cdkBatchMapper.findByIdForUpdate(code.getBatchId());
        if (code != null && batch == null) {
            log.error("cdk redeem rejected after lookup, reason=batch_not_found, userId={}, codeId={}, batchId={}",
                    authUser.getUserId(), code.getId(), code.getBatchId());
        }
        validateRedeemable(code, batch);
        String redeemNo = createNo(REDEEM_NO_PREFIX);
        CdkRedeemRecordEntity record = buildProcessingRecord(authUser, batch, code, request, servletRequest, redeemNo);
        cdkRedeemRecordMapper.insert(record);
        log.info("cdk redeem record created, userId={}, redeemNo={}, batchNo={}, codeId={}",
                authUser.getUserId(), redeemNo, batch.getBatchNo(), code.getId());
        if (cdkCodeMapper.markRedeemed(code.getId(), authUser.getUserId(), redeemNo) == 0) {
            // markRedeemed 带状态条件，返回 0 表示并发下该码已被其他请求抢先兑换。
            log.info("cdk redeem rejected during status update, reason=code_already_changed, userId={}, redeemNo={}, codeId={}",
                    authUser.getUserId(), redeemNo, code.getId());
            recordRedeemFailure(authUser.getUserId());
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        log.info("cdk code marked redeemed, userId={}, redeemNo={}, codeId={}, batchNo={}",
                authUser.getUserId(), redeemNo, code.getId(), batch.getBatchNo());
        PointRechargeOrderEntity rechargeOrder = buildRechargeOrder(authUser, batch, redeemNo, request.getIdempotencyKey());
        pointRechargeOrderMapper.insert(rechargeOrder);
        log.info("cdk point recharge order created, userId={}, redeemNo={}, rechargeNo={}, amount={}",
                authUser.getUserId(), redeemNo, rechargeOrder.getRechargeNo(), rechargeOrder.getAmount());
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
        log.info("cdk benefit grant completed, userId={}, redeemNo={}, grantedPoints={}",
                authUser.getUserId(), redeemNo, grantResult.getGrantedPoints());
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

    /**
     * 构建 CDK 兑换成功的事务消息载荷。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private String buildRedeemOutboxPayload(Long userId,
                                            CdkBatchEntity batch,
                                            String redeemNo,
                                            BenefitGrantResult grantResult) {
        Map<String, Object> payload = new HashMap<>();
        // Outbox 只投递业务定位和发放结果，不包含 CDK 明文，避免消息链路扩大凭证暴露面。
        payload.put("userId", userId);
        payload.put("redeemNo", redeemNo);
        payload.put("batchNo", batch.getBatchNo());
        payload.put("benefitType", batch.getBenefitType());
        payload.put("grantedPoints", grantResult.getGrantedPoints());
        return FastJsonUtils.toJsonString(payload);
    }

    /**
     * 分页查询 CDK 兑换记录。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    @Override
    public PageResponse<CdkRedeemRecordVo> listRedeemRecords(CdkRedeemRecordListRequest request) {
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkRedeemRecordEntity> entities = cdkRedeemRecordMapper.findPage(request.getUserId(), request.getBatchId(),
                request.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = cdkRedeemRecordMapper.countPage(request.getUserId(), request.getBatchId(), request.getStatus());
        return new PageResponse<CdkRedeemRecordVo>(toRedeemRecordVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 分页查询 CDK 明细，并解密返回可复制明文。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    @Override
    public PageResponse<CdkCodeVo> listCodes(CdkCodeListRequest request) {
        if (request.getBatchId() != null && cdkBatchMapper.findById(request.getBatchId()) == null) {
            throw new BusinessException(ErrorCode.CDK_BATCH_NOT_FOUND);
        }
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkCodeEntity> entities = cdkCodeMapper.findPage(request.getKeyword(), request.getBatchId(), request.getStatus(),
                offset(pageNo, pageSize), pageSize);
        long total = cdkCodeMapper.countPage(request.getKeyword(), request.getBatchId(), request.getStatus());
        return new PageResponse<CdkCodeVo>(toCodeVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 管理员变更单个 CDK 状态。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkCodeVo updateCodeStatus(Long id, CdkCodeStatusRequest request) {
        CdkCodeEntity code = cdkCodeMapper.findByIdForUpdate(id);
        if (code == null) {
            log.error("cdk code status update rejected, reason=code_not_found, codeId={}, targetStatus={}, operator={}",
                    id, request.getStatus(), currentUsername());
            throw new BusinessException(ErrorCode.CDK_CODE_NOT_FOUND);
        }
        if (!CdkConstants.CODE_STATUS_ACTIVE.equals(request.getStatus())
                && !CdkConstants.CODE_STATUS_DISABLED.equals(request.getStatus())) {
            log.error("cdk code status update rejected, reason=invalid_target_status, codeId={}, currentStatus={}, targetStatus={}, operator={}",
                    id, code.getStatus(), request.getStatus(), currentUsername());
            throw new BusinessException(ErrorCode.CDK_CODE_STATUS_INVALID);
        }
        if (CdkConstants.CODE_STATUS_REDEEMED.equals(code.getStatus())) {
            log.info("cdk code status update rejected, reason=already_redeemed, codeId={}, targetStatus={}, operator={}",
                    id, request.getStatus(), currentUsername());
            throw new BusinessException(ErrorCode.CDK_CODE_STATUS_INVALID);
        }
        if (CdkConstants.CODE_STATUS_ACTIVE.equals(request.getStatus())) {
            CdkBatchEntity batch = cdkBatchMapper.findByIdForUpdate(code.getBatchId());
            if (batch == null || !CdkConstants.BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
                log.info("cdk code status update rejected, reason=batch_unavailable, codeId={}, batchId={}, targetStatus={}, operator={}",
                        id, code.getBatchId(), request.getStatus(), currentUsername());
                throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
            }
        }
        if (!request.getStatus().equals(code.getStatus())) {
            cdkCodeMapper.updateStatus(id, request.getStatus());
            log.info("cdk code status changed, codeId={}, batchId={}, status={}, operator={}",
                    id, code.getBatchId(), request.getStatus(), currentUsername());
        }
        return toCodeVo(cdkCodeMapper.findById(id));
    }

    /**
     * 批量生成当前批次的 CDK 明文并加密落库。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private List<String> generateCodes(CdkBatchEntity batch) {
        List<String> plainCodes = new ArrayList<String>();
        log.info("cdk code generation started, batchNo={}, batchId={}, totalCount={}, randomBytes={}, codePartCount={}",
                batch.getBatchNo(), batch.getId(), batch.getTotalCount(), CdkConstants.RANDOM_BYTE_LENGTH, CdkConstants.CODE_PART_COUNT);
        for (int index = 0; index < batch.getTotalCount(); index++) {
            String plainCode = generatePlainCode();
            CdkCodeEntity code = new CdkCodeEntity();
            code.setBatchId(batch.getId());
            code.setCodeHash(hmacSha256(plainCode));
            // 明文码以 AES-GCM 加密落库，管理端可重复查看但数据库不可直接读出明文。
            code.setEncryptedCode(encryptPlainCode(plainCode));
            code.setCodePrefix(EMPTY_CODE_PREFIX);
            code.setChecksum(resolveChecksum(plainCode));
            code.setStatus(CdkConstants.CODE_STATUS_ACTIVE);
            code.setVersion(0L);
            cdkCodeMapper.insert(code);
            plainCodes.add(plainCode);
            int generatedCount = index + 1;
            // 大批次生成需要进度日志，便于定位事务执行到哪一段，但不能打印明文 CDK。
            if (generatedCount % CODE_GENERATE_PROGRESS_LOG_INTERVAL == 0 || generatedCount == batch.getTotalCount()) {
                log.info("cdk code generation progress, batchNo={}, generatedCount={}, totalCount={}",
                        batch.getBatchNo(), generatedCount, batch.getTotalCount());
            }
        }
        log.info("cdk code generation completed, batchNo={}, batchId={}, generatedCount={}",
                batch.getBatchNo(), batch.getId(), plainCodes.size());
        return plainCodes;
    }

    /**
     * 构建权益配置快照。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String buildBenefitConfig(CdkBatchCreateRequest request) {
        JSONObject config = new JSONObject();
        // 权益配置使用 JSON 快照落库，后续批次规则调整不会影响已生成 CDK 的兑换结果。
        config.put(CONFIG_POINTS, request.getPoints());
        config.put(CONFIG_GRANT_REASON, GRANT_REASON_CDK);
        config.put(CONFIG_REMARK, request.getRemark());
        return config.toJSONString();
    }

    /**
     * 校验 CDK 和批次是否仍可兑换。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void validateRedeemable(CdkCodeEntity code, CdkBatchEntity batch) {
        if (code == null || batch == null) {
            log.info("cdk redeemability check failed, reason=missing_code_or_batch");
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        if (!CdkConstants.CODE_STATUS_ACTIVE.equals(code.getStatus()) || !CdkConstants.BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
            log.info("cdk redeemability check failed, reason=status_unavailable, codeId={}, codeStatus={}, batchId={}, batchStatus={}",
                    code.getId(), code.getStatus(), batch.getId(), batch.getStatus());
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        // 批次有效期按服务端当前时间判断，客户端时间不参与兑换有效性校验。
        LocalDateTime validFrom = LocalDateTime.parse(batch.getValidFrom(), DATE_TIME_FORMATTER);
        LocalDateTime validTo = LocalDateTime.parse(batch.getValidTo(), DATE_TIME_FORMATTER);
        if (now.isBefore(validFrom) || now.isAfter(validTo)) {
            log.info("cdk redeemability check failed, reason=outside_valid_period, codeId={}, batchNo={}, now={}, validFrom={}, validTo={}",
                    code.getId(), batch.getBatchNo(), now.format(DATE_TIME_FORMATTER), batch.getValidFrom(), batch.getValidTo());
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
    }

    /**
     * 构建兑换处理中记录。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
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

    /**
     * 构建积分充值单。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
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

    /**
     * 加锁读取批次记录。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private CdkBatchEntity loadBatchForUpdate(Long id) {
        CdkBatchEntity batch = cdkBatchMapper.findByIdForUpdate(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.CDK_BATCH_NOT_FOUND);
        }
        return batch;
    }

    /**
     * 执行兑换前风控检查。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void checkRedeemRisk(Long userId, String clientIp) {
        String userKey = String.valueOf(userId);
        if (Boolean.TRUE.equals(redisCacheService.hasKey(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(userKey)))) {
            log.info("cdk redeem rejected by risk lock, userId={}", userId);
            throw new BusinessException(ErrorCode.CDK_REDEEM_LOCKED);
        }
        // 用户和 IP 双维度限流，既限制单用户刷码，也限制同 IP 批量猜码。
        checkLimit(CdkConstants.REDEEM_USER_LIMIT_PREFIX + digestKey(userKey), cdkProperties.getRedeemUserLimit(),
                cdkProperties.getRedeemUserWindowSeconds());
        checkLimit(CdkConstants.REDEEM_IP_LIMIT_PREFIX + digestKey(defaultText(clientIp)), cdkProperties.getRedeemIpLimit(),
                cdkProperties.getRedeemIpWindowSeconds());
    }

    /**
     * 检查单个 Redis 限流窗口。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void checkLimit(String key, long limit, long windowSeconds) {
        Long current = redisCacheService.increment(key);
        if (current != null && current == 1L) {
            // 限流计数器第一次创建时绑定窗口 TTL，后续窗口到期自动归零。
            redisCacheService.expire(key, windowSeconds);
        }
        if (current != null && current > limit) {
            log.info("cdk redeem rejected by rate limit, key={}, current={}, limit={}, windowSeconds={}",
                    key, current, limit, windowSeconds);
            throw new BusinessException(ErrorCode.AUTH_RATE_LIMITED);
        }
    }

    /**
     * 记录兑换失败并在达到阈值后锁定用户。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void recordRedeemFailure(Long userId) {
        String failKey = CdkConstants.REDEEM_FAIL_PREFIX + digestKey(String.valueOf(userId));
        Long current = redisCacheService.increment(failKey);
        if (current != null && current == 1L) {
            // 失败计数的 TTL 与锁定时长一致，低频失败不会永久累计。
            redisCacheService.expire(failKey, cdkProperties.getRedeemLockSeconds());
        }
        log.info("cdk redeem failure recorded, userId={}, currentFailureCount={}, lockThreshold={}",
                userId, current, cdkProperties.getRedeemFailureLimit());
        if (current != null && current >= cdkProperties.getRedeemFailureLimit()) {
            redisCacheService.set(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)), "locked",
                    cdkProperties.getRedeemLockSeconds());
            redisCacheService.delete(failKey);
            createRedeemLockedAlert(userId);
            log.info("cdk redeem locked after repeated failures, userId={}, lockSeconds={}", userId, cdkProperties.getRedeemLockSeconds());
        }
    }

    /**
     * 创建兑换锁定风险告警。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void createRedeemLockedAlert(Long userId) {
        Map<String, Object> detail = new HashMap<>();
        // 风控告警保存锁定参数，便于运营判断是否需要人工解锁或调整阈值。
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

    /**
     * 记录兑换成功后的风控清理。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void recordRedeemSuccess(Long userId) {
        // 成功兑换后清理失败状态，让用户从下一次兑换开始重新计算风控窗口。
        redisCacheService.delete(CdkConstants.REDEEM_FAIL_PREFIX + digestKey(String.valueOf(userId)));
        redisCacheService.delete(CdkConstants.REDEEM_LOCK_PREFIX + digestKey(String.valueOf(userId)));
    }

    /**
     * 标准化用户提交的 CDK。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String normalizeCode(String code) {
        // 支持用户复制时带空格或小写，统一规范化后再做校验和 hash。
        return code == null ? "" : code.trim().replace(" ", "").toUpperCase();
    }

    /**
     * 校验 CDK 展示格式和末尾校验位。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private boolean isCodeFormatValid(String code) {
        String[] parts = code.split(CODE_SEPARATOR);
        // 新格式不再包含固定业务前缀，只允许固定数量的随机分组和末尾校验位。
        if (parts.length != CdkConstants.CODE_PART_COUNT) {
            return false;
        }
        String checksum = parts[parts.length - 1];
        if (checksum.length() != CdkConstants.CHECKSUM_LENGTH) {
            return false;
        }
        for (int index = 0; index < parts.length - CdkConstants.CHECKSUM_PART_COUNT; index++) {
            if (parts[index].length() != CdkConstants.CODE_GROUP_LENGTH) {
                return false;
            }
        }
        // 校验位基于码体和固定盐计算，用于在查库前快速过滤明显伪造的 CDK。
        return checksum.equals(calculateChecksum(resolveCodeBody(code)));
    }

    /**
     * 生成 CDK 明文。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String generatePlainCode() {
        byte[] randomBytes = new byte[CdkConstants.RANDOM_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        // SecureRandom 生成的随机字节是 CDK 安全性的核心来源，固定分隔符只提升可读性。
        String randomPart = toHex(randomBytes).toUpperCase();
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < randomPart.length(); index += CdkConstants.CODE_GROUP_LENGTH) {
            if (builder.length() > 0) {
                builder.append(CODE_SEPARATOR);
            }
            builder.append(randomPart, index, index + CdkConstants.CODE_GROUP_LENGTH);
        }
        String body = builder.toString();
        // 校验位追加在末尾，兑换时可先验格式再查 HMAC，减少数据库压力。
        return body + CODE_SEPARATOR + calculateChecksum(body);
    }

    /**
     * 截取 CDK 中参与校验的码体。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String resolveCodeBody(String code) {
        int lastSeparatorIndex = code.lastIndexOf(CODE_SEPARATOR);
        return lastSeparatorIndex <= 0 ? code : code.substring(0, lastSeparatorIndex);
    }

    /**
     * 截取 CDK 末尾校验位。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String resolveChecksum(String code) {
        int lastSeparatorIndex = code.lastIndexOf(CODE_SEPARATOR);
        return lastSeparatorIndex <= 0 ? "" : code.substring(lastSeparatorIndex + 1);
    }

    /**
     * 计算 CDK 校验位。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String calculateChecksum(String body) {
        return sha256(body + CdkConstants.CHECKSUM_SALT).substring(0, CdkConstants.CHECKSUM_LENGTH).toUpperCase();
    }

    /**
     * 校验 CDK pepper 是否达到最低安全要求。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private void ensurePepperConfigured() {
        if (cdkProperties.getPepper() == null || cdkProperties.getPepper().getBytes(StandardCharsets.UTF_8).length < PEPPER_MIN_LENGTH) {
            log.error("cdk pepper validation failed, reason=missing_or_too_short, minLength={}", PEPPER_MIN_LENGTH);
            throw new BusinessException(ErrorCode.CDK_PEPPER_MISSING);
        }
    }

    /**
     * 计算 CDK 明文的 HMAC 摘要。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String hmacSha256(String value) {
        try {
            // HMAC key 使用外部 pepper，数据库即使泄露也无法直接反推出明文 CDK。
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(cdkProperties.getPepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return toHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            log.error("cdk hmac calculation failed", exception);
            throw new IllegalStateException("cdk hmac failed", exception);
        }
    }

    /**
     * 加密 CDK 明文。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private String encryptPlainCode(String plainCode) {
        try {
            // 每个 CDK 使用独立 IV，AES-GCM 同时提供加密和完整性校验。
            byte[] iv = randomBytes(CODE_IV_LENGTH);
            Cipher cipher = Cipher.getInstance(CODE_ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, codeSecretKey(), new GCMParameterSpec(CODE_GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainCode.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            log.error("cdk plain code encryption failed", exception);
            throw new BusinessException(ErrorCode.CDK_CODE_DECRYPT_FAILED, "CDK 明文加密失败");
        }
    }

    /**
     * 解密 CDK 明文供管理端展示。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private String decryptPlainCode(CdkCodeEntity code) {
        if (!StringUtils.hasText(code.getEncryptedCode())) {
            return "";
        }
        try {
            byte[] payload = Base64.getDecoder().decode(code.getEncryptedCode());
            if (payload.length <= CODE_IV_LENGTH) {
                log.error("cdk plain code decrypt failed, reason=payload_too_short, codeId={}", code.getId());
                throw new BusinessException(ErrorCode.CDK_CODE_DECRYPT_FAILED);
            }
            byte[] iv = new byte[CODE_IV_LENGTH];
            byte[] cipherText = new byte[payload.length - CODE_IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, CODE_IV_LENGTH);
            System.arraycopy(payload, CODE_IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(CODE_ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, codeSecretKey(), new GCMParameterSpec(CODE_GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            log.error("cdk plain code decrypt failed, codeId={}", code.getId(), exception);
            throw new BusinessException(ErrorCode.CDK_CODE_DECRYPT_FAILED);
        }
    }

    /**
     * 基于 pepper 派生 CDK 明文加密密钥。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private SecretKeySpec codeSecretKey() {
        byte[] digest = sha256Bytes(cdkProperties.getPepper() + CODE_ENCRYPTION_CONTEXT);
        return new SecretKeySpec(digest, CODE_SECRET_ALGORITHM);
    }

    /**
     * 生成 Redis key 后缀摘要。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String digestKey(String value) {
        return sha256(defaultText(value));
    }

    /**
     * 计算字符串 SHA-256 摘要。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String sha256(String value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256_ALGORITHM).digest(defaultText(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk sha256 failed", exception);
        }
    }

    /**
     * 计算字节数组 SHA-256 摘要。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String sha256(byte[] value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256_ALGORITHM).digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk sha256 failed", exception);
        }
    }

    /**
     * 计算字符串 SHA-256 原始字节摘要。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance(SHA_256_ALGORITHM).digest(defaultText(value).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk sha256 failed", exception);
        }
    }

    /**
     * 生成指定长度的安全随机字节。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * 将字节数组转为小写十六进制字符串。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            int value = bytes[index] & BYTE_MASK;
            chars[index * 2] = HEX_DIGITS[value >>> HEX_RADIX_SHIFT];
            chars[index * 2 + 1] = HEX_DIGITS[value & LOW_NIBBLE_MASK];
        }
        return new String(chars);
    }

    /**
     * 生成业务流水号。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    /**
     * 获取当前链路追踪 ID。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return StringUtils.hasText(traceId) ? traceId : "";
    }

    /**
     * 返回非空文本或默认占位值。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN_TARGET;
    }

    /**
     * 标准化页码。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private int pageNo(Integer value) {
        return value == null || value < CdkConstants.DEFAULT_PAGE_NO ? CdkConstants.DEFAULT_PAGE_NO : value;
    }

    /**
     * 标准化分页大小。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private int pageSize(Integer value) {
        if (value == null || value < CdkConstants.DEFAULT_PAGE_NO) {
            return CdkConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(value, CdkConstants.MAX_PAGE_SIZE);
    }

    /**
     * 计算分页偏移量。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private int offset(int pageNo, int pageSize) {
        return (pageNo - CdkConstants.DEFAULT_PAGE_NO) * pageSize;
    }

    /**
     * 将兑换记录转换为兑换结果。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
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

    /**
     * 转换 CDK 批次视图对象。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
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

    /**
     * 批量转换 CDK 批次视图对象。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private List<CdkBatchVo> toBatchVoList(List<CdkBatchEntity> entities) {
        List<CdkBatchVo> records = new ArrayList<CdkBatchVo>();
        for (CdkBatchEntity entity : entities) {
            records.add(toBatchVo(entity));
        }
        return records;
    }

    /**
     * 转换 CDK 兑换记录视图对象。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
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

    /**
     * 批量转换 CDK 兑换记录视图对象。
     * 创建日期：2026-04-24
     * author：sunshengxian
     */
    private List<CdkRedeemRecordVo> toRedeemRecordVoList(List<CdkRedeemRecordEntity> entities) {
        List<CdkRedeemRecordVo> records = new ArrayList<CdkRedeemRecordVo>();
        for (CdkRedeemRecordEntity entity : entities) {
            records.add(toRedeemRecordVo(entity));
        }
        return records;
    }

    /**
     * 转换 CDK 明细视图对象。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private CdkCodeVo toCodeVo(CdkCodeEntity entity) {
        CdkCodeVo vo = new CdkCodeVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setBatchId(String.valueOf(entity.getBatchId()));
        vo.setBatchNo(entity.getBatchNo());
        vo.setBatchName(entity.getBatchName());
        vo.setBenefitType(entity.getBenefitType());
        vo.setBenefitConfig(entity.getBenefitConfig());
        vo.setBatchStatus(entity.getBatchStatus());
        vo.setValidFrom(entity.getValidFrom());
        vo.setValidTo(entity.getValidTo());
        vo.setCdk(decryptPlainCode(entity));
        vo.setCodePrefix(entity.getCodePrefix());
        vo.setChecksum(entity.getChecksum());
        vo.setStatus(entity.getStatus());
        vo.setRedeemedUserId(entity.getRedeemedUserId() == null ? "" : String.valueOf(entity.getRedeemedUserId()));
        vo.setRedeemedAt(entity.getRedeemedAt());
        vo.setRedeemRecordNo(entity.getRedeemRecordNo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 批量转换 CDK 明细视图对象。
     * 创建日期：2026-04-26
     * author：sunshengxian
     */
    private List<CdkCodeVo> toCodeVoList(List<CdkCodeEntity> entities) {
        List<CdkCodeVo> records = new ArrayList<CdkCodeVo>();
        for (CdkCodeEntity entity : entities) {
            records.add(toCodeVo(entity));
        }
        return records;
    }
}
