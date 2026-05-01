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
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkDisableRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkExtendRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkListRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialExtractAccessRecordEntity;
import com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.entity.CredentialOperationAuditEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractAccessRecordMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import com.winsalty.quickstart.credential.vo.CredentialExtractAccessRecordVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCopyVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCreateResultVo;
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
    private static final String OPERATION_REISSUE = "extract_link_reissue";
    private static final String TARGET_TYPE_EXTRACT_LINK = "extract_link";
    private static final String TOKEN_URL_PATH = "/credentials/extract/";
    private static final String TOKEN_KEY_ID_DEFAULT = "default";
    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String AES_ALGORITHM = "AES";
    private static final int AES_GCM_TAG_BITS = 128;
    private static final int AES_GCM_IV_BYTES = 12;
    private static final int AES_KEY_128_BYTES = 16;
    private static final int AES_KEY_192_BYTES = 24;
    private static final int AES_KEY_256_BYTES = 32;
    private static final int UUID_FRAGMENT_LENGTH = 12;
    private static final int TOKEN_UUID_COUNT = 2;
    private static final int FIRST_SORT_NO = 1;
    private static final int DEFAULT_EXTRACT_EXPIRE_DAYS = 7;

    private final CredentialBatchMapper credentialBatchMapper;
    private final CredentialItemMapper credentialItemMapper;
    private final CredentialExtractLinkMapper credentialExtractLinkMapper;
    private final CredentialExtractLinkItemMapper credentialExtractLinkItemMapper;
    private final CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper;
    private final CredentialOperationAuditMapper credentialOperationAuditMapper;
    private final CredentialProperties credentialProperties;
    private final CredentialCryptoService credentialCryptoService;

    public CredentialExtractLinkServiceImpl(CredentialBatchMapper credentialBatchMapper,
                                            CredentialItemMapper credentialItemMapper,
                                            CredentialExtractLinkMapper credentialExtractLinkMapper,
                                            CredentialExtractLinkItemMapper credentialExtractLinkItemMapper,
                                            CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper,
                                            CredentialOperationAuditMapper credentialOperationAuditMapper,
                                            CredentialProperties credentialProperties,
                                            CredentialCryptoService credentialCryptoService) {
        this.credentialBatchMapper = credentialBatchMapper;
        this.credentialItemMapper = credentialItemMapper;
        this.credentialExtractLinkMapper = credentialExtractLinkMapper;
        this.credentialExtractLinkItemMapper = credentialExtractLinkItemMapper;
        this.credentialExtractAccessRecordMapper = credentialExtractAccessRecordMapper;
        this.credentialOperationAuditMapper = credentialOperationAuditMapper;
        this.credentialProperties = credentialProperties;
        this.credentialCryptoService = credentialCryptoService;
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

    /**
     * 按批次批量生成提取链接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkCreateResultVo createBatchLinks(Long batchId, CredentialExtractLinkCreateRequest request) {
        CredentialBatchEntity batch = credentialBatchMapper.findByIdForUpdate(batchId);
        if (batch == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND);
        }
        if (!CredentialConstants.STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_STATUS_INVALID);
        }
        CredentialExtractLinkCreateRequest command = normalizeCreateRequest(request);
        List<CredentialItemEntity> items = credentialItemMapper.findActiveByBatchForUpdate(batchId, credentialProperties.getMaxBatchSize());
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_ITEM_NOT_FOUND, "没有可生成链接的凭证明细");
        }
        CredentialExtractLinkCreateResultVo result = createLinksForItems(batch, items, command, true);
        log.info("credential extract links created by batch, batchId={}, linkCount={}, itemCount={}",
                batchId, result.getLinkCount(), result.getItemCount());
        return result;
    }

    /**
     * 按单个凭证明细生成提取链接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkCreateResultVo createItemLink(Long itemId, CredentialExtractLinkCreateRequest request) {
        CredentialItemEntity item = credentialItemMapper.findByIdForUpdate(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_ITEM_NOT_FOUND);
        }
        if (!CredentialConstants.STATUS_ACTIVE.equals(item.getStatus())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_ITEM_STATUS_INVALID);
        }
        CredentialBatchEntity batch = credentialBatchMapper.findByIdForUpdate(item.getBatchId());
        if (batch == null || !CredentialConstants.STATUS_ACTIVE.equals(batch.getStatus())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_STATUS_INVALID);
        }
        CredentialExtractLinkCreateRequest command = normalizeCreateRequest(request);
        List<CredentialItemEntity> items = new ArrayList<CredentialItemEntity>();
        items.add(item);
        return createLinksForItems(batch, items, command, true);
    }

    /**
     * 补发提取链接并停用旧链接。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialExtractLinkCopyVo reissueLink(Long id, CredentialExtractLinkCreateRequest request, HttpServletRequest servletRequest) {
        CredentialExtractLinkEntity before = loadLinkForUpdate(id);
        CredentialBatchEntity batch = credentialBatchMapper.findByIdForUpdate(before.getBatchId());
        if (batch == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND);
        }
        List<Long> itemIds = credentialExtractLinkItemMapper.findItemIdsByLinkId(id);
        if (itemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_ITEM_NOT_FOUND);
        }
        String ids = joinIds(itemIds);
        List<CredentialItemEntity> items = credentialItemMapper.findByUnsafeIds(ids);
        CredentialExtractLinkCreateRequest command = normalizeCreateRequest(request);
        credentialExtractLinkMapper.disable(id, currentUserId(), "补发链接自动停用");
        CredentialExtractLinkCreateResultVo result = createLinksForItems(batch, items, command, false);
        CredentialExtractLinkCopyVo copyVo = result.getLinks().get(0);
        CredentialExtractLinkEntity after = loadLink(Long.valueOf(copyVo.getId()));
        audit(before, OPERATION_REISSUE, after, true, "", servletRequest);
        log.info("credential extract link reissued, oldLinkId={}, newLinkId={}, operator={}", id, copyVo.getId(), currentUserId());
        return copyVo;
    }

    private CredentialExtractLinkCreateResultVo createLinksForItems(CredentialBatchEntity batch,
                                                                    List<CredentialItemEntity> items,
                                                                    CredentialExtractLinkCreateRequest request,
                                                                    boolean updateItemStatus) {
        List<CredentialExtractLinkCopyVo> links = new ArrayList<CredentialExtractLinkCopyVo>();
        int cursor = 0;
        while (cursor < items.size()) {
            int end = Math.min(cursor + request.getItemsPerLink(), items.size());
            List<CredentialItemEntity> group = items.subList(cursor, end);
            CredentialExtractLinkCopyVo copyVo = createSingleLink(batch, group, request, updateItemStatus);
            links.add(copyVo);
            cursor = end;
        }
        CredentialExtractLinkCreateResultVo result = new CredentialExtractLinkCreateResultVo();
        result.setLinkCount(links.size());
        result.setItemCount(items.size());
        result.setLinks(links);
        return result;
    }

    private CredentialExtractLinkCopyVo createSingleLink(CredentialBatchEntity batch,
                                                        List<CredentialItemEntity> items,
                                                        CredentialExtractLinkCreateRequest request,
                                                        boolean updateItemStatus) {
        String token = createToken();
        CredentialExtractLinkEntity link = new CredentialExtractLinkEntity();
        link.setLinkNo(createNo("CEL"));
        link.setCategoryId(batch.getCategoryId());
        link.setBatchId(batch.getId());
        link.setTokenHash(credentialCryptoService.hmacToken(token));
        link.setEncryptedToken(credentialCryptoService.encryptToken(token));
        link.setTokenKeyId(TOKEN_KEY_ID_DEFAULT);
        link.setItemCount(items.size());
        link.setMaxAccessCount(request.getMaxAccessCount());
        link.setAccessedCount(0);
        link.setExpireAt(request.getExpireAt());
        link.setStatus(CredentialConstants.STATUS_ACTIVE);
        link.setCreatedBy(currentUserId());
        link.setRemark(request.getRemark() == null ? "" : request.getRemark());
        credentialExtractLinkMapper.insert(link);
        int sortNo = FIRST_SORT_NO;
        for (CredentialItemEntity item : items) {
            credentialExtractLinkItemMapper.insert(link.getId(), item.getId(), batch.getId(), sortNo++);
            if (updateItemStatus && CredentialConstants.FULFILLMENT_TYPE_TEXT_SECRET.equals(batch.getFulfillmentType())) {
                credentialItemMapper.markLinked(item.getId());
            }
        }
        int availableDelta = CredentialConstants.FULFILLMENT_TYPE_TEXT_SECRET.equals(batch.getFulfillmentType()) && updateItemStatus ? items.size() : 0;
        credentialBatchMapper.increaseLinked(batch.getId(), items.size(), availableDelta);
        CredentialExtractLinkCopyVo vo = new CredentialExtractLinkCopyVo();
        vo.setId(String.valueOf(link.getId()));
        vo.setLinkNo(link.getLinkNo());
        vo.setUrl(buildPublicUrl(token));
        return vo;
    }

    private CredentialExtractLinkCreateRequest normalizeCreateRequest(CredentialExtractLinkCreateRequest request) {
        CredentialExtractLinkCreateRequest command = request == null ? new CredentialExtractLinkCreateRequest() : request;
        if (command.getItemsPerLink() == null || command.getItemsPerLink() <= 0
                || command.getItemsPerLink() > credentialProperties.getExtract().getMaxItemsPerLink()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_LIMIT_INVALID, "单链接凭证数量不合法");
        }
        if (command.getMaxAccessCount() == null || command.getMaxAccessCount() <= 0
                || command.getMaxAccessCount() > credentialProperties.getExtract().getMaxAccessCount()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_LIMIT_INVALID);
        }
        if (!StringUtils.hasText(command.getExpireAt())) {
            command.setExpireAt(LocalDateTime.now().plusDays(DEFAULT_EXTRACT_EXPIRE_DAYS).format(DATE_TIME_FORMATTER));
        }
        LocalDateTime expireAt = parseDateTime(command.getExpireAt());
        LocalDateTime maxExpireAt = LocalDateTime.now().plusDays(credentialProperties.getExtract().getMaxExpireDays());
        if (!expireAt.isAfter(LocalDateTime.now()) || expireAt.isAfter(maxExpireAt)) {
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_EXPIRED, "提取链接过期时间不合法");
        }
        return command;
    }

    private String createToken() {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < TOKEN_UUID_COUNT; index++) {
            builder.append(UUID.randomUUID().toString().replace("-", ""));
        }
        return builder.toString();
    }

    private String joinIds(List<Long> ids) {
        StringBuilder builder = new StringBuilder();
        for (Long id : ids) {
            if (id == null || id <= 0L) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(id);
        }
        return builder.toString();
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
