package com.winsalty.quickstart.credential.service.impl;

import com.alibaba.fastjson2.JSON;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialExtractAccessRecordListRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkDisableRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkExtendRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkListRequest;
import com.winsalty.quickstart.credential.entity.CredentialExtractAccessRecordEntity;
import com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.entity.CredentialOperationAuditEntity;
import com.winsalty.quickstart.credential.mapper.CredentialExtractAccessRecordMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.vo.CredentialExtractAccessRecordVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCopyVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 凭证提取链接服务实现。
 * 实现提取链接管理页的查询、复制、停用、延期和审计落库逻辑。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Service
public class CredentialExtractLinkServiceImpl extends BaseService implements CredentialExtractLinkService {

    private static final Logger log = LoggerFactory.getLogger(CredentialExtractLinkServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String AUDIT_NO_PREFIX = "COA";
    private static final String OPERATION_COPY_URL = "extract_link_copy_url";
    private static final String OPERATION_DISABLE = "extract_link_disable";
    private static final String OPERATION_EXTEND = "extract_link_extend";
    private static final String TARGET_TYPE_EXTRACT_LINK = "extract_link";
    private static final String TOKEN_URL_PATH = "/credentials/extract/";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String AES_ALGORITHM = "AES";
    private static final int AES_GCM_TAG_BITS = 128;
    private static final int AES_GCM_IV_BYTES = 12;
    private static final int AES_KEY_128_BYTES = 16;
    private static final int AES_KEY_192_BYTES = 24;
    private static final int AES_KEY_256_BYTES = 32;
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final CredentialExtractLinkMapper credentialExtractLinkMapper;
    private final CredentialExtractLinkItemMapper credentialExtractLinkItemMapper;
    private final CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper;
    private final CredentialOperationAuditMapper credentialOperationAuditMapper;
    private final CredentialProperties credentialProperties;

    public CredentialExtractLinkServiceImpl(CredentialExtractLinkMapper credentialExtractLinkMapper,
                                            CredentialExtractLinkItemMapper credentialExtractLinkItemMapper,
                                            CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper,
                                            CredentialOperationAuditMapper credentialOperationAuditMapper,
                                            CredentialProperties credentialProperties) {
        this.credentialExtractLinkMapper = credentialExtractLinkMapper;
        this.credentialExtractLinkItemMapper = credentialExtractLinkItemMapper;
        this.credentialExtractAccessRecordMapper = credentialExtractAccessRecordMapper;
        this.credentialOperationAuditMapper = credentialOperationAuditMapper;
        this.credentialProperties = credentialProperties;
    }

    /**
     * 分页查询提取链接。
     */
    @Override
    public PageResponse<CredentialExtractLinkVo> listLinks(CredentialExtractLinkListRequest request) {
        CredentialExtractLinkListRequest query = request == null ? new CredentialExtractLinkListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNo - 1) * pageSize;
        List<CredentialExtractLinkEntity> entities = credentialExtractLinkMapper.findPage(
                query.getKeyword(), query.getCategoryId(), query.getBatchId(), query.getStatus(), query.getCreatedBy(),
                query.getExpireFrom(), query.getExpireTo(), query.getLastAccessFrom(), query.getLastAccessTo(), offset, pageSize);
        long total = credentialExtractLinkMapper.countPage(query.getKeyword(), query.getCategoryId(), query.getBatchId(),
                query.getStatus(), query.getCreatedBy(), query.getExpireFrom(), query.getExpireTo(),
                query.getLastAccessFrom(), query.getLastAccessTo());
        log.info("credential extract link page loaded, total={}, pageNo={}, pageSize={}", total, pageNo, pageSize);
        return new PageResponse<CredentialExtractLinkVo>(toLinkVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 查询提取链接详情。
     */
    @Override
    public CredentialExtractLinkVo getLink(Long id) {
        CredentialExtractLinkEntity entity = loadLink(id);
        log.info("credential extract link detail loaded, linkNo={}, linkId={}", entity.getLinkNo(), entity.getId());
        return toLinkVo(entity);
    }

    /**
     * 查询提取链接包含的凭证明细。
     */
    @Override
    public List<CredentialItemVo> listItems(Long id) {
        loadLink(id);
        List<CredentialItemEntity> entities = credentialExtractLinkItemMapper.findItemsByLinkId(id);
        log.info("credential extract link items loaded, linkId={}, itemSize={}", id, entities.size());
        return toItemVoList(entities);
    }

    /**
     * 查询提取链接访问记录。
     */
    @Override
    public PageResponse<CredentialExtractAccessRecordVo> listAccessRecords(Long id, CredentialExtractAccessRecordListRequest request) {
        loadLink(id);
        CredentialExtractAccessRecordListRequest query = request == null ? new CredentialExtractAccessRecordListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNo - 1) * pageSize;
        List<CredentialExtractAccessRecordEntity> entities = credentialExtractAccessRecordMapper.findPage(id, query.getSuccess(),
                query.getFingerprint(), offset, pageSize);
        long total = credentialExtractAccessRecordMapper.countPage(id, query.getSuccess(), query.getFingerprint());
        log.info("credential extract access page loaded, linkId={}, total={}", id, total);
        return new PageResponse<CredentialExtractAccessRecordVo>(toAccessVoList(entities), pageNo, pageSize, total);
    }

    /**
     * 复制提取链接公开 URL。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkCopyVo copyUrl(Long id, HttpServletRequest servletRequest) {
        CredentialExtractLinkEntity entity = loadLink(id);
        if (!CredentialConstants.STATUS_ACTIVE.equals(entity.getStatus())) {
            audit(entity, OPERATION_COPY_URL, null, false, ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED.getMessage(), servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED);
        }
        if (!StringUtils.hasText(entity.getEncryptedToken())) {
            audit(entity, OPERATION_COPY_URL, null, false, ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING.getMessage(), servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING, "该链接未保存可复制 token，请补发链接");
        }
        String token = decryptToken(entity.getEncryptedToken());
        CredentialExtractLinkCopyVo vo = new CredentialExtractLinkCopyVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setLinkNo(entity.getLinkNo());
        vo.setUrl(buildPublicUrl(token));
        audit(entity, OPERATION_COPY_URL, null, true, "", servletRequest);
        log.info("credential extract link url copied, linkNo={}, linkId={}, operator={}",
                entity.getLinkNo(), entity.getId(), currentUserId());
        return vo;
    }

    /**
     * 停用提取链接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkVo disableLink(Long id, CredentialExtractLinkDisableRequest request, HttpServletRequest servletRequest) {
        CredentialExtractLinkEntity before = loadLinkForUpdate(id);
        if (!CredentialConstants.STATUS_ACTIVE.equals(before.getStatus())) {
            audit(before, OPERATION_DISABLE, null, false, ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED.getMessage(), servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED);
        }
        String reason = request == null || !StringUtils.hasText(request.getReason()) ? "管理员手动停用" : request.getReason().trim();
        credentialExtractLinkMapper.disable(id, currentUserId(), reason);
        CredentialExtractLinkEntity after = loadLink(id);
        audit(before, OPERATION_DISABLE, after, true, "", servletRequest);
        log.info("credential extract link disabled, linkNo={}, linkId={}, operator={}",
                before.getLinkNo(), before.getId(), currentUserId());
        return toLinkVo(after);
    }

    /**
     * 延期提取链接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkVo extendLink(Long id, CredentialExtractLinkExtendRequest request, HttpServletRequest servletRequest) {
        CredentialExtractLinkEntity before = loadLinkForUpdate(id);
        if (!CredentialConstants.STATUS_ACTIVE.equals(before.getStatus())) {
            audit(before, OPERATION_EXTEND, null, false, ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED.getMessage(), servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED);
        }
        LocalDateTime expireAt = parseDateTime(request.getExpireAt());
        if (!expireAt.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_EXPIRED, "延期时间必须晚于当前时间");
        }
        credentialExtractLinkMapper.extend(id, expireAt.format(DATE_TIME_FORMATTER));
        CredentialExtractLinkEntity after = loadLink(id);
        audit(before, OPERATION_EXTEND, after, true, "", servletRequest);
        log.info("credential extract link extended, linkNo={}, linkId={}, expireAt={}, operator={}",
                before.getLinkNo(), before.getId(), after.getExpireAt(), currentUserId());
        return toLinkVo(after);
    }

    private CredentialExtractLinkEntity loadLink(Long id) {
        if (id == null || id <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
        CredentialExtractLinkEntity entity = credentialExtractLinkMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_NOT_FOUND);
        }
        return entity;
    }

    private CredentialExtractLinkEntity loadLinkForUpdate(Long id) {
        if (id == null || id <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
        CredentialExtractLinkEntity entity = credentialExtractLinkMapper.findByIdForUpdate(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_NOT_FOUND);
        }
        return entity;
    }

    private void audit(CredentialExtractLinkEntity before,
                       String operationType,
                       CredentialExtractLinkEntity after,
                       boolean success,
                       String failureReason,
                       HttpServletRequest servletRequest) {
        CredentialOperationAuditEntity entity = new CredentialOperationAuditEntity();
        entity.setAuditNo(createNo(AUDIT_NO_PREFIX));
        entity.setOperatorId(currentUserId());
        entity.setOperationType(operationType);
        entity.setTargetType(TARGET_TYPE_EXTRACT_LINK);
        entity.setTargetId(before.getId());
        entity.setBeforeSnapshot(toAuditSnapshot(before));
        entity.setAfterSnapshot(after == null ? null : toAuditSnapshot(after));
        entity.setClientIp(IpUtils.getClientIp(servletRequest));
        entity.setSuccess(success ? 1 : 0);
        entity.setFailureReason(failureReason == null ? "" : failureReason);
        credentialOperationAuditMapper.insert(entity);
    }

    private String toAuditSnapshot(CredentialExtractLinkEntity entity) {
        CredentialExtractLinkVo vo = toLinkVo(entity);
        return JSON.toJSONString(vo);
    }

    private String buildPublicUrl(String token) {
        String baseUrl = credentialProperties.getExtract().getPublicBaseUrl();
        String normalizedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "";
        while (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        return normalizedBaseUrl + TOKEN_URL_PATH + token;
    }

    private String decryptToken(String encryptedToken) {
        String key = credentialProperties.getExtract().getTokenEncryptionKey();
        if (!isValidAesKey(key)) {
            log.error("credential extract token encryption key validation failed, reason=missing_or_invalid");
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING);
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encryptedToken);
            if (raw.length <= AES_GCM_IV_BYTES) {
                throw new IllegalArgumentException("invalid encrypted token length");
            }
            byte[] iv = new byte[AES_GCM_IV_BYTES];
            byte[] cipherText = new byte[raw.length - AES_GCM_IV_BYTES];
            System.arraycopy(raw, 0, iv, 0, AES_GCM_IV_BYTES);
            System.arraycopy(raw, AES_GCM_IV_BYTES, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM),
                    new GCMParameterSpec(AES_GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("credential extract token decrypt failed, reason={}", ex.getMessage());
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_SECRET_MISSING, "提取链接 token 解密失败");
        }
    }

    private boolean isValidAesKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        int length = key.getBytes(StandardCharsets.UTF_8).length;
        return length == AES_KEY_128_BYTES || length == AES_KEY_192_BYTES || length == AES_KEY_256_BYTES;
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private List<CredentialExtractLinkVo> toLinkVoList(List<CredentialExtractLinkEntity> entities) {
        List<CredentialExtractLinkVo> records = new ArrayList<CredentialExtractLinkVo>();
        for (CredentialExtractLinkEntity entity : entities) {
            records.add(toLinkVo(entity));
        }
        return records;
    }

    private CredentialExtractLinkVo toLinkVo(CredentialExtractLinkEntity entity) {
        CredentialExtractLinkVo vo = new CredentialExtractLinkVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setLinkNo(entity.getLinkNo());
        vo.setCategoryId(entity.getCategoryId() == null ? null : String.valueOf(entity.getCategoryId()));
        vo.setCategoryName(entity.getCategoryName());
        vo.setBatchId(entity.getBatchId() == null ? null : String.valueOf(entity.getBatchId()));
        vo.setBatchNo(entity.getBatchNo());
        vo.setBatchName(entity.getBatchName());
        vo.setItemCount(entity.getItemCount());
        vo.setMaxAccessCount(entity.getMaxAccessCount());
        vo.setAccessedCount(entity.getAccessedCount());
        vo.setRemainingAccessCount(Math.max(0, defaultInt(entity.getMaxAccessCount()) - defaultInt(entity.getAccessedCount())));
        vo.setExpireAt(entity.getExpireAt());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()));
        vo.setDisabledBy(entity.getDisabledBy() == null ? null : String.valueOf(entity.getDisabledBy()));
        vo.setDisabledAt(entity.getDisabledAt());
        vo.setLastAccessedAt(entity.getLastAccessedAt());
        vo.setRemark(entity.getRemark());
        vo.setCopyable(StringUtils.hasText(entity.getEncryptedToken()));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<CredentialItemVo> toItemVoList(List<CredentialItemEntity> entities) {
        List<CredentialItemVo> records = new ArrayList<CredentialItemVo>();
        for (CredentialItemEntity entity : entities) {
            CredentialItemVo vo = new CredentialItemVo();
            vo.setId(String.valueOf(entity.getId()));
            vo.setBatchId(entity.getBatchId() == null ? null : String.valueOf(entity.getBatchId()));
            vo.setCategoryId(entity.getCategoryId() == null ? null : String.valueOf(entity.getCategoryId()));
            vo.setItemNo(entity.getItemNo());
            vo.setSecretMask(entity.getSecretMask());
            vo.setChecksum(entity.getChecksum());
            vo.setSourceType(entity.getSourceType());
            vo.setStatus(entity.getStatus());
            vo.setConsumedUserId(entity.getConsumedUserId() == null ? null : String.valueOf(entity.getConsumedUserId()));
            vo.setConsumedAt(entity.getConsumedAt());
            vo.setConsumeBizNo(entity.getConsumeBizNo());
            vo.setCreatedAt(entity.getCreatedAt());
            vo.setUpdatedAt(entity.getUpdatedAt());
            records.add(vo);
        }
        return records;
    }

    private List<CredentialExtractAccessRecordVo> toAccessVoList(List<CredentialExtractAccessRecordEntity> entities) {
        List<CredentialExtractAccessRecordVo> records = new ArrayList<CredentialExtractAccessRecordVo>();
        for (CredentialExtractAccessRecordEntity entity : entities) {
            CredentialExtractAccessRecordVo vo = new CredentialExtractAccessRecordVo();
            vo.setId(String.valueOf(entity.getId()));
            vo.setAccessNo(entity.getAccessNo());
            vo.setLinkId(entity.getLinkId() == null ? null : String.valueOf(entity.getLinkId()));
            vo.setBatchId(entity.getBatchId() == null ? null : String.valueOf(entity.getBatchId()));
            vo.setItemCount(entity.getItemCount());
            vo.setSuccess(Integer.valueOf(1).equals(entity.getSuccess()));
            vo.setFailureReason(entity.getFailureReason());
            vo.setClientIp(entity.getClientIp());
            vo.setUserAgentHash(entity.getUserAgentHash());
            vo.setBrowserFingerprint(entity.getBrowserFingerprint());
            vo.setDeviceSnapshot(entity.getDeviceSnapshot());
            vo.setTraceId(entity.getTraceId());
            vo.setCreatedAt(entity.getCreatedAt());
            records.add(vo);
        }
        return records;
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < CredentialConstants.DEFAULT_PAGE_NO ? CredentialConstants.DEFAULT_PAGE_NO : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < CredentialConstants.DEFAULT_PAGE_NO) {
            return CredentialConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CredentialConstants.MAX_PAGE_SIZE);
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(NO_TIME_FORMATTER) + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }
}
