package com.winsalty.quickstart.credential.service.impl;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractAccessRecordMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialExtractLinkMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
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
    private static final long USER_ID = 1001L;
    private static final String USERNAME = "admin";
    private static final String ROLE_CODE = "admin";
    private static final String SESSION_ID = "session-extract-link-test";

    /**
     * 清理认证上下文，避免 ThreadLocal 状态影响其他测试。
     */
    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

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

    /**
     * 验证全部 Key 一个链接模式会用批次可用明细数量覆盖每链接数量。
     */
    @Test
    void createBatchLinksShouldPutAllItemsIntoOneLinkWhenScopeIsAllInOne() {
        AuthContext.set(new AuthUser(USER_ID, USERNAME, ROLE_CODE, SESSION_ID));
        CredentialBatchMapper batchMapper = mock(CredentialBatchMapper.class);
        CredentialItemMapper itemMapper = mock(CredentialItemMapper.class);
        CredentialExtractLinkMapper linkMapper = mock(CredentialExtractLinkMapper.class);
        CredentialExtractLinkItemMapper linkItemMapper = mock(CredentialExtractLinkItemMapper.class);
        CredentialExtractAccessRecordMapper accessRecordMapper = mock(CredentialExtractAccessRecordMapper.class);
        CredentialOperationAuditMapper auditMapper = mock(CredentialOperationAuditMapper.class);
        CredentialProperties properties = new CredentialProperties();
        properties.setSecretPepper("1234567890123456");
        properties.setSecretEncryptionKey("1234567890123456");
        properties.getExtract().setTokenSecret("1234567890123456");
        properties.getExtract().setTokenEncryptionKey("1234567890123456");
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        CredentialExtractLinkServiceImpl service = new CredentialExtractLinkServiceImpl(batchMapper, itemMapper, linkMapper,
                linkItemMapper, accessRecordMapper, auditMapper, properties, cryptoService);
        CredentialBatchEntity batch = new CredentialBatchEntity();
        batch.setId(BATCH_ID);
        batch.setCategoryId(2001L);
        batch.setFulfillmentType(CredentialConstants.FULFILLMENT_TYPE_POINTS_REDEEM);
        batch.setStatus(CredentialConstants.STATUS_ACTIVE);
        batch.setLinkedCount(0);
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(batch);
        when(itemMapper.findActiveByBatchForUpdate(BATCH_ID, properties.getMaxBatchSize()))
                .thenReturn(Arrays.asList(item(1L), item(2L), item(3L)));
        when(cryptoService.hmacToken(any())).thenReturn("token-hash");
        when(cryptoService.encryptToken(any())).thenReturn("encrypted-token");
        CredentialExtractLinkCreateRequest request = new CredentialExtractLinkCreateRequest();
        request.setItemScope(CredentialConstants.ITEM_SCOPE_ALL_IN_ONE);

        service.createBatchLinks(BATCH_ID, request);

        ArgumentCaptor<com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity> linkCaptor =
                ArgumentCaptor.forClass(com.winsalty.quickstart.credential.entity.CredentialExtractLinkEntity.class);
        verify(linkMapper).insert(linkCaptor.capture());
        assertEquals(3, linkCaptor.getValue().getItemCount());
    }

    /**
     * 构造可用于提取链接生成的凭证明细。
     */
    private CredentialItemEntity item(Long id) {
        CredentialItemEntity entity = new CredentialItemEntity();
        entity.setId(id);
        entity.setStatus(CredentialConstants.STATUS_ACTIVE);
        return entity;
    }
}
