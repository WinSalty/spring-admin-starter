package com.winsalty.quickstart.credential.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractAccessRecordMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 凭证提取链接服务测试。
 * 覆盖批次重复生成提取链接的拦截规则，避免一个批次被重复分发。
 * 创建日期：2026-05-02
 * author：sunshengxian
 */
class CredentialExtractLinkServiceImplTest {

    private static final long BATCH_ID = 3001L;

    /**
     * 验证已经生成过提取链接的批次不能再次按批次生成链接。
     */
    @Test
    void createBatchLinksShouldRejectBatchThatAlreadyHasExtractLinks() {
        CredentialBatchMapper batchMapper = mock(CredentialBatchMapper.class);
        CredentialItemMapper itemMapper = mock(CredentialItemMapper.class);
        CredentialExtractLinkMapper linkMapper = mock(CredentialExtractLinkMapper.class);
        CredentialExtractLinkItemMapper linkItemMapper = mock(CredentialExtractLinkItemMapper.class);
        CredentialExtractAccessRecordMapper accessRecordMapper = mock(CredentialExtractAccessRecordMapper.class);
        CredentialOperationAuditMapper auditMapper = mock(CredentialOperationAuditMapper.class);
        CredentialProperties properties = new CredentialProperties();
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        CredentialExtractLinkServiceImpl service = new CredentialExtractLinkServiceImpl(batchMapper, itemMapper, linkMapper,
                linkItemMapper, accessRecordMapper, auditMapper, properties, cryptoService);
        CredentialBatchEntity batch = new CredentialBatchEntity();
        batch.setId(BATCH_ID);
        batch.setStatus(CredentialConstants.STATUS_ACTIVE);
        batch.setLinkedCount(1);
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(batch);
        CredentialExtractLinkCreateRequest request = new CredentialExtractLinkCreateRequest();

        assertThrows(BusinessException.class, () -> service.createBatchLinks(BATCH_ID, request));

        verify(itemMapper, never()).findActiveByBatchForUpdate(anyLong(), anyInt());
    }
}
