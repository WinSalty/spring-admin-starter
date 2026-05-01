package com.winsalty.quickstart.cdk.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.winsalty.quickstart.cdk.config.CdkProperties;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRecordListRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractAccessRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkExtractLinkDisableRequest;
import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import com.winsalty.quickstart.cdk.entity.CdkExtractAccessRecordEntity;
import com.winsalty.quickstart.cdk.entity.CdkExtractLinkEntity;
import com.winsalty.quickstart.cdk.mapper.CdkBatchMapper;
import com.winsalty.quickstart.cdk.mapper.CdkCodeMapper;
import com.winsalty.quickstart.cdk.mapper.CdkExtractAccessRecordMapper;
import com.winsalty.quickstart.cdk.mapper.CdkExtractLinkMapper;
import com.winsalty.quickstart.cdk.service.CdkExtractService;
import com.winsalty.quickstart.cdk.vo.CdkBatchExtractLinkVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractAccessRecordVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractLinkVo;
import com.winsalty.quickstart.cdk.vo.CdkExtractViewVo;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.infra.json.FastJsonUtils;
import com.winsalty.quickstart.infra.web.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * CDK 提取链接服务实现。
 * 实现临时 URL 生成、访问次数控制、公开 CDK 提取和设备访问审计。
 * 创建日期：2026-04-30
 * author：sunshengxian
 */
@Service
public class CdkExtractServiceImpl extends BaseService implements CdkExtractService {

    private static final Logger log = LoggerFactory.getLogger(CdkExtractServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String LINK_NO_PREFIX = "CEL";
    private static final String ACCESS_NO_PREFIX = "CEA";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_DISABLED = "disabled";
    private static final String STATUS_EXHAUSTED = "exhausted";
    private static final String ACCESS_RESULT_SUCCESS = "success";
    private static final String ACCESS_RESULT_FAILED = "failed";
    private static final String TOKEN_HASH_CONTEXT = ":cdk-extract-link:v1";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final String CODE_ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final String CODE_SECRET_ALGORITHM = "AES";
    private static final String CODE_ENCRYPTION_CONTEXT = ":cdk-code:v1";
    private static final String UA_HEADER = "User-Agent";
    private static final String REFERER_HEADER = "Referer";
    private static final String UNKNOWN_TARGET = "unknown";
    private static final String CONFIG_POINTS = "points";
    private static final String BENEFIT_POINTS_UNIT = " 积分";
    private static final String EMPTY_TEXT = "";
    private static final int TOKEN_RANDOM_BYTES = 32;
    private static final int SECRET_MIN_LENGTH = 32;
    private static final int CODE_IV_LENGTH = 12;
    private static final int CODE_GCM_TAG_LENGTH_BITS = 128;
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int HEX_RADIX_SHIFT = 4;
    private static final int LOW_NIBBLE_MASK = 0x0F;
    private static final int BYTE_MASK = 0xFF;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();
    private final CdkBatchMapper cdkBatchMapper;
    private final CdkCodeMapper cdkCodeMapper;
    private final CdkExtractLinkMapper cdkExtractLinkMapper;
    private final CdkExtractAccessRecordMapper cdkExtractAccessRecordMapper;
    private final CdkProperties cdkProperties;

    /**
     * 构造 CDK 提取链接服务依赖。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    public CdkExtractServiceImpl(CdkBatchMapper cdkBatchMapper,
                                 CdkCodeMapper cdkCodeMapper,
                                 CdkExtractLinkMapper cdkExtractLinkMapper,
                                 CdkExtractAccessRecordMapper cdkExtractAccessRecordMapper,
                                 CdkProperties cdkProperties) {
        this.cdkBatchMapper = cdkBatchMapper;
        this.cdkCodeMapper = cdkCodeMapper;
        this.cdkExtractLinkMapper = cdkExtractLinkMapper;
        this.cdkExtractAccessRecordMapper = cdkExtractAccessRecordMapper;
        this.cdkProperties = cdkProperties;
    }

    /**
     * 创建单个 CDK 的临时提取链接。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkExtractLinkVo createLink(Long codeId, CdkExtractLinkCreateRequest request) {
        ensureExtractSecretConfigured();
        CdkCodeEntity code = loadExtractableCode(codeId);
        LocalDateTime expireAt = parseExpireAt(request.getExpireAt());
        validateExpireAt(expireAt);
        int maxAccessCount = validateMaxAccessCount(request.getMaxAccessCount());
        return createLinkForCode(code, expireAt, maxAccessCount, request.getRemark());
    }

    /**
     * 一次性创建批次内所有可提取 CDK 的临时提取链接。
     * 创建日期：2026-05-01
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkBatchExtractLinkVo createBatchLinks(Long batchId, CdkExtractLinkCreateRequest request) {
        ensureExtractSecretConfigured();
        CdkBatchEntity batch = cdkBatchMapper.findById(batchId);
        validateExtractableBatch(batch);
        LocalDateTime expireAt = parseExpireAt(request.getExpireAt());
        validateExpireAt(expireAt);
        int maxAccessCount = validateMaxAccessCount(request.getMaxAccessCount());
        List<CdkCodeEntity> codes = cdkCodeMapper.findActiveWithBatchByBatchId(batchId);
        List<CdkExtractLinkVo> links = new ArrayList<CdkExtractLinkVo>();
        for (CdkCodeEntity code : codes) {
            // 批量生成只为当前仍可提取的 active CDK 创建链接，已兑换或已失效 CDK 自动跳过。
            links.add(createLinkForCode(code, expireAt, maxAccessCount, request.getRemark()));
        }
        CdkBatchExtractLinkVo vo = new CdkBatchExtractLinkVo();
        vo.setBatchId(String.valueOf(batch.getId()));
        vo.setBatchNo(batch.getBatchNo());
        vo.setBatchName(batch.getBatchName());
        vo.setGeneratedCount(links.size());
        vo.setSkippedCount(Math.max(0, batch.getTotalCount() - links.size()));
        vo.setLinks(links);
        log.info("cdk batch extract links created, batchNo={}, batchId={}, generatedCount={}, skippedCount={}, operator={}",
                batch.getBatchNo(), batch.getId(), vo.getGeneratedCount(), vo.getSkippedCount(), currentUsername());
        return vo;
    }

    /**
     * 查询单个 CDK 的提取链接历史。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Override
    public List<CdkExtractLinkVo> listLinks(Long codeId) {
        if (cdkCodeMapper.findById(codeId) == null) {
            throw new BusinessException(ErrorCode.CDK_CODE_NOT_FOUND);
        }
        List<CdkExtractLinkEntity> entities = cdkExtractLinkMapper.findByCodeId(codeId);
        List<CdkExtractLinkVo> records = new ArrayList<CdkExtractLinkVo>();
        for (CdkExtractLinkEntity entity : entities) {
            records.add(toLinkVo(entity));
        }
        return records;
    }

    /**
     * 停用提取链接。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CdkExtractLinkVo disableLink(Long linkId, CdkExtractLinkDisableRequest request) {
        CdkExtractLinkEntity entity = cdkExtractLinkMapper.findByIdForUpdate(linkId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_NOT_FOUND);
        }
        if (!STATUS_ACTIVE.equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_DISABLED);
        }
        cdkExtractLinkMapper.disable(linkId, currentUsername(), request.getReason());
        log.info("cdk extract link disabled, linkNo={}, linkId={}, operator={}",
                entity.getLinkNo(), linkId, currentUsername());
        return toLinkVo(cdkExtractLinkMapper.findById(linkId));
    }

    /**
     * 分页查询提取链接访问记录。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Override
    public PageResponse<CdkExtractAccessRecordVo> listAccessRecords(Long linkId, CdkExtractAccessRecordListRequest request) {
        if (cdkExtractLinkMapper.findById(linkId) == null) {
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_NOT_FOUND);
        }
        int pageNo = pageNo(request.getPageNo());
        int pageSize = pageSize(request.getPageSize());
        List<CdkExtractAccessRecordEntity> entities = cdkExtractAccessRecordMapper.findPage(linkId, request.getResult(),
                request.getFingerprint(), offset(pageNo, pageSize), pageSize);
        long total = cdkExtractAccessRecordMapper.countPage(linkId, request.getResult(), request.getFingerprint());
        List<CdkExtractAccessRecordVo> records = new ArrayList<CdkExtractAccessRecordVo>();
        for (CdkExtractAccessRecordEntity entity : entities) {
            records.add(toAccessRecordVo(entity));
        }
        return new PageResponse<CdkExtractAccessRecordVo>(records, pageNo, pageSize, total);
    }

    /**
     * 公开访问 CDK 提取链接。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public CdkExtractViewVo extract(String token, CdkExtractAccessRequest request, HttpServletRequest servletRequest) {
        ensureExtractSecretConfigured();
        String tokenHash = tokenHash(token);
        CdkExtractLinkEntity link = cdkExtractLinkMapper.findByTokenHashForUpdate(tokenHash);
        if (link == null) {
            recordAccess(null, null, null, ACCESS_RESULT_FAILED, "not_found",
                    ErrorCode.CDK_EXTRACT_LINK_NOT_FOUND.getMessage(), request, servletRequest);
            log.info("cdk extract rejected, reason=link_not_found, tokenHash={}", tokenHash);
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_NOT_FOUND, "链接无效或已过期");
        }
        validateLinkUsable(link, request, servletRequest);
        CdkCodeEntity code;
        try {
            code = loadExtractableCode(link.getCodeId());
        } catch (BusinessException exception) {
            recordAccess(link, link.getCodeId(), link.getBatchId(), ACCESS_RESULT_FAILED, "code_unavailable",
                    exception.getMessage(), request, servletRequest);
            throw exception;
        }
        if (cdkExtractLinkMapper.incrementAccessed(link.getId()) == 0) {
            recordAccess(link, link.getCodeId(), link.getBatchId(), ACCESS_RESULT_FAILED, "exhausted",
                    ErrorCode.CDK_EXTRACT_LINK_EXHAUSTED.getMessage(), request, servletRequest);
            log.info("cdk extract rejected, reason=access_exhausted, linkNo={}, linkId={}", link.getLinkNo(), link.getId());
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_EXHAUSTED);
        }
        CdkExtractLinkEntity updatedLink = cdkExtractLinkMapper.findById(link.getId());
        recordAccess(updatedLink, updatedLink.getCodeId(), updatedLink.getBatchId(), ACCESS_RESULT_SUCCESS,
                EMPTY_TEXT, EMPTY_TEXT, request, servletRequest);
        String plainCode = decryptPlainCode(code);
        CdkExtractViewVo vo = new CdkExtractViewVo();
        vo.setCdk(plainCode);
        vo.setBatchName(code.getBatchName());
        vo.setBenefitType(code.getBenefitType());
        vo.setBenefitText(resolveBenefitText(code.getBenefitConfig()));
        vo.setValidTo(code.getValidTo());
        vo.setRemainingAccessCount(Math.max(0, updatedLink.getMaxAccessCount() - updatedLink.getAccessedCount()));
        vo.setStatus(STATUS_ACTIVE);
        log.info("cdk extracted, linkNo={}, codeId={}, batchId={}, remainingAccessCount={}",
                updatedLink.getLinkNo(), code.getId(), code.getBatchId(), vo.getRemainingAccessCount());
        return vo;
    }

    /**
     * 校验并加载可提取的 CDK 明细。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private CdkCodeEntity loadExtractableCode(Long codeId) {
        CdkCodeEntity code = cdkCodeMapper.findWithBatchById(codeId);
        if (code == null) {
            throw new BusinessException(ErrorCode.CDK_CODE_NOT_FOUND);
        }
        if (!CdkConstants.CODE_STATUS_ACTIVE.equals(code.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
        if (!CdkConstants.BATCH_STATUS_ACTIVE.equals(code.getBatchStatus())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        validateBatchValidPeriod(code);
        return code;
    }

    /**
     * 校验批次是否允许批量生成提取链接。
     * 创建日期：2026-05-01
     * author：sunshengxian
     */
    private void validateExtractableBatch(CdkBatchEntity batch) {
        if (batch == null) {
            throw new BusinessException(ErrorCode.CDK_BATCH_NOT_FOUND);
        }
        if (!CdkConstants.BATCH_STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validFrom = parseDateTime(batch.getValidFrom(), ErrorCode.CDK_BATCH_STATUS_INVALID);
        LocalDateTime validTo = parseDateTime(batch.getValidTo(), ErrorCode.CDK_BATCH_STATUS_INVALID);
        if (now.isBefore(validFrom) || now.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.CDK_BATCH_STATUS_INVALID);
        }
    }

    /**
     * 为单个已校验 CDK 创建提取链接。
     * 创建日期：2026-05-01
     * author：sunshengxian
     */
    private CdkExtractLinkVo createLinkForCode(CdkCodeEntity code,
                                               LocalDateTime expireAt,
                                               int maxAccessCount,
                                               String remark) {
        String token = createToken();
        CdkExtractLinkEntity entity = new CdkExtractLinkEntity();
        entity.setLinkNo(createNo(LINK_NO_PREFIX));
        entity.setCodeId(code.getId());
        entity.setBatchId(code.getBatchId());
        entity.setTokenHash(tokenHash(token));
        entity.setMaxAccessCount(maxAccessCount);
        entity.setAccessedCount(0);
        entity.setExpireAt(expireAt.format(DATE_TIME_FORMATTER));
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedBy(currentUsername());
        entity.setRemark(defaultEmpty(remark));
        cdkExtractLinkMapper.insert(entity);
        log.info("cdk extract link created, linkNo={}, codeId={}, batchId={}, maxAccessCount={}, operator={}",
                entity.getLinkNo(), code.getId(), code.getBatchId(), maxAccessCount, currentUsername());
        CdkExtractLinkVo vo = toLinkVo(cdkExtractLinkMapper.findById(entity.getId()));
        vo.setUrl(buildPublicUrl(token));
        return vo;
    }

    /**
     * 校验提取链接状态、过期时间和剩余访问次数。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private void validateLinkUsable(CdkExtractLinkEntity link,
                                    CdkExtractAccessRequest request,
                                    HttpServletRequest servletRequest) {
        if (STATUS_DISABLED.equals(link.getStatus())) {
            recordAccess(link, link.getCodeId(), link.getBatchId(), ACCESS_RESULT_FAILED, "disabled",
                    ErrorCode.CDK_EXTRACT_LINK_DISABLED.getMessage(), request, servletRequest);
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_DISABLED);
        }
        if (STATUS_EXHAUSTED.equals(link.getStatus()) || link.getAccessedCount() >= link.getMaxAccessCount()) {
            recordAccess(link, link.getCodeId(), link.getBatchId(), ACCESS_RESULT_FAILED, "exhausted",
                    ErrorCode.CDK_EXTRACT_LINK_EXHAUSTED.getMessage(), request, servletRequest);
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_EXHAUSTED);
        }
        LocalDateTime expireAt = parseDateTime(link.getExpireAt(), ErrorCode.CDK_EXTRACT_LINK_EXPIRED);
        if (LocalDateTime.now().isAfter(expireAt)) {
            recordAccess(link, link.getCodeId(), link.getBatchId(), ACCESS_RESULT_FAILED, "expired",
                    ErrorCode.CDK_EXTRACT_LINK_EXPIRED.getMessage(), request, servletRequest);
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_EXPIRED, "链接无效或已过期");
        }
    }

    /**
     * 校验批次当前是否在有效期内。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private void validateBatchValidPeriod(CdkCodeEntity code) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validFrom = parseDateTime(code.getValidFrom(), ErrorCode.CDK_CODE_UNAVAILABLE);
        LocalDateTime validTo = parseDateTime(code.getValidTo(), ErrorCode.CDK_CODE_UNAVAILABLE);
        if (now.isBefore(validFrom) || now.isAfter(validTo)) {
            throw new BusinessException(ErrorCode.CDK_CODE_UNAVAILABLE);
        }
    }

    /**
     * 记录提取访问审计。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private void recordAccess(CdkExtractLinkEntity link,
                              Long codeId,
                              Long batchId,
                              String result,
                              String failureCode,
                              String failureMessage,
                              CdkExtractAccessRequest request,
                              HttpServletRequest servletRequest) {
        CdkExtractAccessRecordEntity record = new CdkExtractAccessRecordEntity();
        record.setAccessNo(createNo(ACCESS_NO_PREFIX));
        record.setLinkId(link == null ? null : link.getId());
        record.setCodeId(codeId);
        record.setBatchId(batchId);
        record.setResult(result);
        record.setFailureCode(defaultEmpty(failureCode));
        record.setFailureMessage(defaultEmpty(failureMessage));
        record.setClientIp(IpUtils.getClientIp(servletRequest));
        record.setUserAgentHash(sha256(servletRequest == null ? EMPTY_TEXT : servletRequest.getHeader(UA_HEADER)));
        record.setBrowserFingerprint(defaultEmpty(request == null ? EMPTY_TEXT : request.getBrowserFingerprint()));
        record.setDeviceSnapshot(FastJsonUtils.toJsonString(request == null ? null : request.getDeviceSnapshot()));
        record.setReferer(defaultEmpty(servletRequest == null ? EMPTY_TEXT : servletRequest.getHeader(REFERER_HEADER)));
        record.setTraceId(currentTraceId());
        cdkExtractAccessRecordMapper.insert(record);
        log.info("cdk extract access recorded, accessNo={}, linkNo={}, result={}, clientIpDigest={}, fingerprint={}",
                record.getAccessNo(), link == null ? EMPTY_TEXT : link.getLinkNo(), result,
                sha256(defaultEmpty(record.getClientIp())), record.getBrowserFingerprint());
    }

    /**
     * 校验提取链接 token 密钥是否配置。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private void ensureExtractSecretConfigured() {
        String tokenSecret = cdkProperties.getExtract().getTokenSecret();
        if (tokenSecret == null || tokenSecret.getBytes(StandardCharsets.UTF_8).length < SECRET_MIN_LENGTH) {
            log.error("cdk extract token secret validation failed, reason=missing_or_too_short, minLength={}", SECRET_MIN_LENGTH);
            throw new BusinessException(ErrorCode.CDK_EXTRACT_SECRET_MISSING);
        }
        if (cdkProperties.getPepper() == null || cdkProperties.getPepper().getBytes(StandardCharsets.UTF_8).length < SECRET_MIN_LENGTH) {
            log.error("cdk pepper validation failed for extract, reason=missing_or_too_short, minLength={}", SECRET_MIN_LENGTH);
            throw new BusinessException(ErrorCode.CDK_PEPPER_MISSING);
        }
    }

    /**
     * 校验最大访问次数。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private int validateMaxAccessCount(Integer maxAccessCount) {
        int limit = Math.min(cdkProperties.getExtract().getMaxAccessCount(), 100);
        if (maxAccessCount == null || maxAccessCount < 1 || maxAccessCount > limit) {
            throw new BusinessException(ErrorCode.CDK_EXTRACT_LINK_LIMIT_INVALID);
        }
        return maxAccessCount;
    }

    /**
     * 校验提取链接过期时间。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private void validateExpireAt(LocalDateTime expireAt) {
        LocalDateTime now = LocalDateTime.now();
        if (!expireAt.isAfter(now)) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID.getCode(), "过期时间必须晚于当前时间");
        }
        if (expireAt.isAfter(now.plusDays(cdkProperties.getExtract().getMaxExpireDays()))) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID.getCode(), "过期时间超过允许范围");
        }
    }

    /**
     * 解析创建请求中的过期时间。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private LocalDateTime parseExpireAt(String value) {
        return parseDateTime(value, ErrorCode.REQUEST_PARAM_INVALID);
    }

    /**
     * 按统一格式解析日期时间。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private LocalDateTime parseDateTime(String value, ErrorCode errorCode) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            log.error("cdk extract datetime parse failed, value={}, errorCode={}", value, errorCode.getCode());
            throw new BusinessException(errorCode);
        }
    }

    /**
     * 生成公开提取 URL。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String buildPublicUrl(String token) {
        String baseUrl = cdkProperties.getExtract().getPublicBaseUrl();
        String normalizedBaseUrl = StringUtils.hasText(baseUrl) && baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : defaultText(baseUrl);
        return normalizedBaseUrl + "/cdk/extract/" + token;
    }

    /**
     * 生成 URL 安全 token。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String createToken() {
        byte[] bytes = new byte[TOKEN_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 计算 token 摘要。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String tokenHash(String token) {
        return sha256(defaultText(token) + cdkProperties.getExtract().getTokenSecret() + TOKEN_HASH_CONTEXT);
    }

    /**
     * 解密 CDK 明文。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String decryptPlainCode(CdkCodeEntity code) {
        if (!StringUtils.hasText(code.getEncryptedCode())) {
            return EMPTY_TEXT;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(code.getEncryptedCode());
            if (payload.length <= CODE_IV_LENGTH) {
                log.error("cdk extract plain code decrypt failed, reason=payload_too_short, codeId={}", code.getId());
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
            log.error("cdk extract plain code decrypt failed, codeId={}", code.getId(), exception);
            throw new BusinessException(ErrorCode.CDK_CODE_DECRYPT_FAILED);
        }
    }

    /**
     * 基于 pepper 派生 CDK 明文加密密钥。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private SecretKeySpec codeSecretKey() {
        return new SecretKeySpec(sha256Bytes(cdkProperties.getPepper() + CODE_ENCRYPTION_CONTEXT), CODE_SECRET_ALGORITHM);
    }

    /**
     * 解析权益展示文案。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String resolveBenefitText(String benefitConfig) {
        if (!StringUtils.hasText(benefitConfig)) {
            return EMPTY_TEXT;
        }
        try {
            JSONObject config = JSON.parseObject(benefitConfig);
            long points = config.getLongValue(CONFIG_POINTS);
            return String.format("%,d", points) + BENEFIT_POINTS_UNIT;
        } catch (RuntimeException exception) {
            log.error("cdk extract benefit text parse failed, benefitConfig={}", benefitConfig);
            return EMPTY_TEXT;
        }
    }

    /**
     * 转换提取链接视图对象。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private CdkExtractLinkVo toLinkVo(CdkExtractLinkEntity entity) {
        CdkExtractLinkVo vo = new CdkExtractLinkVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setLinkNo(entity.getLinkNo());
        vo.setCodeId(String.valueOf(entity.getCodeId()));
        vo.setBatchId(String.valueOf(entity.getBatchId()));
        vo.setUrl(EMPTY_TEXT);
        vo.setMaxAccessCount(entity.getMaxAccessCount());
        vo.setAccessedCount(entity.getAccessedCount());
        vo.setRemainingAccessCount(Math.max(0, entity.getMaxAccessCount() - entity.getAccessedCount()));
        vo.setExpireAt(entity.getExpireAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setDisabledBy(entity.getDisabledBy());
        vo.setDisabledAt(entity.getDisabledAt());
        vo.setRemark(entity.getRemark());
        vo.setLastAccessedAt(entity.getLastAccessedAt());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 转换访问记录视图对象。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private CdkExtractAccessRecordVo toAccessRecordVo(CdkExtractAccessRecordEntity entity) {
        CdkExtractAccessRecordVo vo = new CdkExtractAccessRecordVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setAccessNo(entity.getAccessNo());
        vo.setLinkId(entity.getLinkId() == null ? EMPTY_TEXT : String.valueOf(entity.getLinkId()));
        vo.setCodeId(entity.getCodeId() == null ? EMPTY_TEXT : String.valueOf(entity.getCodeId()));
        vo.setBatchId(entity.getBatchId() == null ? EMPTY_TEXT : String.valueOf(entity.getBatchId()));
        vo.setResult(entity.getResult());
        vo.setFailureCode(entity.getFailureCode());
        vo.setFailureMessage(entity.getFailureMessage());
        vo.setClientIp(entity.getClientIp());
        vo.setUserAgentHash(entity.getUserAgentHash());
        vo.setBrowserFingerprint(entity.getBrowserFingerprint());
        vo.setDeviceSnapshot(entity.getDeviceSnapshot());
        vo.setReferer(entity.getReferer());
        vo.setTraceId(entity.getTraceId());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 计算字符串 SHA-256 摘要。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String sha256(String value) {
        try {
            return toHex(MessageDigest.getInstance(SHA_256_ALGORITHM).digest(defaultText(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk extract sha256 failed", exception);
        }
    }

    /**
     * 计算字符串 SHA-256 原始字节摘要。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance(SHA_256_ALGORITHM).digest(defaultText(value).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("cdk extract sha256 failed", exception);
        }
    }

    /**
     * 将字节数组转为小写十六进制字符串。
     * 创建日期：2026-04-30
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
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    /**
     * 获取当前链路追踪 ID。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String currentTraceId() {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return StringUtils.hasText(traceId) ? traceId : EMPTY_TEXT;
    }

    /**
     * 返回非空文本或 unknown 占位。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : UNKNOWN_TARGET;
    }

    /**
     * 返回非空文本或空字符串。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private String defaultEmpty(String value) {
        return StringUtils.hasText(value) ? value : EMPTY_TEXT;
    }

    /**
     * 标准化页码。
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private int pageNo(Integer value) {
        return value == null || value < CdkConstants.DEFAULT_PAGE_NO ? CdkConstants.DEFAULT_PAGE_NO : value;
    }

    /**
     * 标准化分页大小。
     * 创建日期：2026-04-30
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
     * 创建日期：2026-04-30
     * author：sunshengxian
     */
    private int offset(int pageNo, int pageSize) {
        return (pageNo - CdkConstants.DEFAULT_PAGE_NO) * pageSize;
    }
}
