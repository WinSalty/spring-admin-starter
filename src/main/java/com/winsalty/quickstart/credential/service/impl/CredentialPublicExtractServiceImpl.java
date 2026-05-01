package com.winsalty.quickstart.credential.service.impl;

import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialPublicExtractRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialExtractAccessRecordEntity;
import com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractAccessRecordMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.service.CredentialPublicExtractService;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import com.winsalty.quickstart.credential.vo.CredentialPublicExtractItemVo;
import com.winsalty.quickstart.credential.vo.CredentialPublicExtractVo;
import com.winsalty.quickstart.infra.web.TraceIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 公开凭证提取服务实现。
 * 校验提取链接状态、访问次数和有效期，成功后返回凭证明文并写访问审计。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Service
public class CredentialPublicExtractServiceImpl extends BaseService implements CredentialPublicExtractService {

    private static final Logger log = LoggerFactory.getLogger(CredentialPublicExtractServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ACCESS_NO_PREFIX = "CEA";
    private static final String FAILURE_NOT_FOUND = "not_found";
    private static final String FAILURE_DISABLED = "disabled";
    private static final String FAILURE_EXPIRED = "expired";
    private static final String FAILURE_EXHAUSTED = "exhausted";
    private static final String COPY_LABEL_CDK = "CDK";
    private static final String COPY_LABEL_SECRET = "卡密";
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final CredentialExtractLinkMapper credentialExtractLinkMapper;
    private final CredentialExtractLinkItemMapper credentialExtractLinkItemMapper;
    private final CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper;
    private final CredentialItemMapper credentialItemMapper;
    private final CredentialBatchMapper credentialBatchMapper;
    private final CredentialCryptoService credentialCryptoService;

    public CredentialPublicExtractServiceImpl(CredentialExtractLinkMapper credentialExtractLinkMapper,
                                              CredentialExtractLinkItemMapper credentialExtractLinkItemMapper,
                                              CredentialExtractAccessRecordMapper credentialExtractAccessRecordMapper,
                                              CredentialItemMapper credentialItemMapper,
                                              CredentialBatchMapper credentialBatchMapper,
                                              CredentialCryptoService credentialCryptoService) {
        this.credentialExtractLinkMapper = credentialExtractLinkMapper;
        this.credentialExtractLinkItemMapper = credentialExtractLinkItemMapper;
        this.credentialExtractAccessRecordMapper = credentialExtractAccessRecordMapper;
        this.credentialItemMapper = credentialItemMapper;
        this.credentialBatchMapper = credentialBatchMapper;
        this.credentialCryptoService = credentialCryptoService;
    }

    /**
     * 公开提取凭证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialPublicExtractVo extract(String token, CredentialPublicExtractRequest request, HttpServletRequest servletRequest) {
        String tokenHash = credentialCryptoService.hmacToken(token);
        CredentialExtractLinkEntity link = credentialExtractLinkMapper.findByTokenHashForUpdate(tokenHash);
        if (link == null) {
            writeAccess(null, 0, false, FAILURE_NOT_FOUND, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_NOT_FOUND);
        }
        validateLink(link, request, servletRequest);
        List<Long> itemIds = credentialExtractLinkItemMapper.findItemIdsByLinkId(link.getId());
        List<CredentialItemEntity> items = credentialItemMapper.findByUnsafeIds(joinIds(itemIds));
        CredentialBatchEntity batch = credentialBatchMapper.findByIdForUpdate(link.getBatchId());
        if (batch == null) {
            writeAccess(link, 0, false, FAILURE_NOT_FOUND, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND);
        }
        credentialExtractLinkMapper.increaseAccess(link.getId());
        String accessNo = createNo(ACCESS_NO_PREFIX);
        if (CredentialConstants.FULFILLMENT_TYPE_TEXT_SECRET.equals(batch.getFulfillmentType())) {
            int firstExtractCount = 0;
            for (CredentialItemEntity item : items) {
                firstExtractCount += credentialItemMapper.markExtracted(item.getId(), accessNo);
            }
            if (firstExtractCount > 0) {
                credentialBatchMapper.increaseConsumed(batch.getId(), firstExtractCount, 0);
            }
        }
        writeAccess(link, items.size(), true, "", request, servletRequest);
        log.info("credential public extract success, linkNo={}, itemCount={}", link.getLinkNo(), items.size());
        return toPublicVo(link, batch, items);
    }

    private void validateLink(CredentialExtractLinkEntity link, CredentialPublicExtractRequest request, HttpServletRequest servletRequest) {
        if (CredentialConstants.STATUS_DISABLED.equals(link.getStatus())) {
            writeAccess(link, 0, false, FAILURE_DISABLED, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED);
        }
        if (CredentialConstants.LINK_STATUS_EXHAUSTED.equals(link.getStatus())
                || link.getAccessedCount() >= link.getMaxAccessCount()) {
            writeAccess(link, 0, false, FAILURE_EXHAUSTED, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_EXHAUSTED);
        }
        if (!CredentialConstants.STATUS_ACTIVE.equals(link.getStatus())) {
            writeAccess(link, 0, false, FAILURE_DISABLED, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_DISABLED);
        }
        if (LocalDateTime.parse(link.getExpireAt(), DATE_TIME_FORMATTER).isBefore(LocalDateTime.now())) {
            writeAccess(link, 0, false, FAILURE_EXPIRED, request, servletRequest);
            throw new BusinessException(ErrorCode.CREDENTIAL_EXTRACT_LINK_EXPIRED);
        }
    }

    private CredentialPublicExtractVo toPublicVo(CredentialExtractLinkEntity link,
                                                 CredentialBatchEntity batch,
                                                 List<CredentialItemEntity> items) {
        List<CredentialPublicExtractItemVo> itemVos = new ArrayList<CredentialPublicExtractItemVo>();
        for (int index = 0; index < items.size(); index++) {
            CredentialItemEntity item = items.get(index);
            CredentialPublicExtractItemVo itemVo = new CredentialPublicExtractItemVo();
            itemVo.setItemNo(item.getItemNo());
            itemVo.setSecretText(credentialCryptoService.decryptSecret(item.getEncryptedSecret()));
            itemVo.setCopyLabel(copyLabel(batch.getFulfillmentType()) + " " + (index + 1));
            itemVos.add(itemVo);
        }
        CredentialPublicExtractVo vo = new CredentialPublicExtractVo();
        vo.setLinkNo(link.getLinkNo());
        vo.setCategoryName(batch.getCategoryName());
        vo.setFulfillmentType(batch.getFulfillmentType());
        vo.setBatchName(batch.getBatchName());
        vo.setRemainingAccessCount(Math.max(0, link.getMaxAccessCount() - link.getAccessedCount() - 1));
        vo.setExpireAt(link.getExpireAt());
        vo.setItems(itemVos);
        return vo;
    }

    private String copyLabel(String fulfillmentType) {
        return CredentialConstants.FULFILLMENT_TYPE_POINTS_REDEEM.equals(fulfillmentType) ? COPY_LABEL_CDK : COPY_LABEL_SECRET;
    }

    private void writeAccess(CredentialExtractLinkEntity link,
                             int itemCount,
                             boolean success,
                             String failureReason,
                             CredentialPublicExtractRequest request,
                             HttpServletRequest servletRequest) {
        CredentialExtractAccessRecordEntity entity = new CredentialExtractAccessRecordEntity();
        entity.setAccessNo(createNo(ACCESS_NO_PREFIX));
        entity.setLinkId(link == null ? null : link.getId());
        entity.setBatchId(link == null ? null : link.getBatchId());
        entity.setItemCount(itemCount);
        entity.setSuccess(success ? 1 : 0);
        entity.setFailureReason(failureReason == null ? "" : failureReason);
        entity.setClientIp(IpUtils.getClientIp(servletRequest));
        entity.setUserAgentHash(credentialCryptoService.sha256(servletRequest.getHeader("User-Agent")));
        entity.setBrowserFingerprint(request == null ? "" : nullToEmpty(request.getBrowserFingerprint()));
        entity.setDeviceSnapshot(request == null ? "" : nullToEmpty(request.getDeviceSnapshot()));
        entity.setTraceId(StringUtils.hasText(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)) ? MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY) : "");
        credentialExtractAccessRecordMapper.insert(entity);
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(NO_TIME_FORMATTER) + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }
}
