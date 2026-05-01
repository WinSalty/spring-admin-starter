package com.winsalty.quickstart.credential.service.impl;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportConfirmRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialCategoryEntity;
import com.winsalty.quickstart.credential.entity.CredentialImportTaskEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialCategoryMapper;
import com.winsalty.quickstart.credential.mapper.CredentialImportTaskMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.mapper.CredentialRedeemRecordMapper;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCreateResultVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端凭证中心服务测试。
 * 覆盖卡密导入后自动生成提取链接的默认过期时间传递，避免导入层破坏链接层默认永久有效规则。
 * 创建日期：2026-05-02
 * author：sunshengxian
 */
class CredentialAdminServiceImplTest {

    private static final long USER_ID = 1001L;
    private static final long CATEGORY_ID = 2001L;
    private static final long BATCH_ID = 3001L;
    private static final long TASK_ID = 4001L;
    private static final long ITEM_ID = 5001L;
    private static final String USERNAME = "admin";
    private static final String ROLE_CODE = "admin";
    private static final String SESSION_ID = "session-credential-admin-test";
    private static final String CARD_SECRET = "CARD-SECRET-0001";
    private static final String NEWLINE_DELIMITER = "\\n";
    private static final String DEFAULT_CATEGORY_CODE = CredentialConstants.CATEGORY_TEXT_CARD_SECRET;
    private static final String CATEGORY_NAME = "文本卡密";
    private static final String SECRET_HASH = "hash-card-secret";
    private static final String SECRET_CIPHER = "cipher-card-secret";
    private static final String SECRET_MASK = "CARD****0001";
    private static final String SECRET_CHECKSUM = "ABCD1234";
    private static final String IMPORT_HASH = "import-hash";

    /**
     * 清理认证上下文，避免 ThreadLocal 状态影响其他测试。
     */
    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    /**
     * 验证导入后生成链接时空过期时间保持为空，由提取链接服务统一应用默认规则。
     */
    @Test
    void confirmImportShouldKeepBlankExtractExpireAtForDefaultPermanentLink() {
        AuthContext.set(new AuthUser(USER_ID, USERNAME, ROLE_CODE, SESSION_ID));
        CredentialCategoryMapper categoryMapper = mock(CredentialCategoryMapper.class);
        CredentialBatchMapper batchMapper = mock(CredentialBatchMapper.class);
        CredentialItemMapper itemMapper = mock(CredentialItemMapper.class);
        CredentialImportTaskMapper importTaskMapper = mock(CredentialImportTaskMapper.class);
        CredentialRedeemRecordMapper redeemRecordMapper = mock(CredentialRedeemRecordMapper.class);
        CredentialOperationAuditMapper operationAuditMapper = mock(CredentialOperationAuditMapper.class);
        CredentialExtractLinkService extractLinkService = mock(CredentialExtractLinkService.class);
        CredentialCryptoService cryptoService = mock(CredentialCryptoService.class);
        CredentialProperties properties = new CredentialProperties();
        CredentialAdminServiceImpl service = new CredentialAdminServiceImpl(categoryMapper, batchMapper, itemMapper,
                importTaskMapper, redeemRecordMapper, operationAuditMapper, extractLinkService, cryptoService, properties);
        CredentialCategoryEntity category = textSecretCategory();
        CredentialBatchEntity persistedBatch = persistedBatch();
        when(categoryMapper.findByCode(DEFAULT_CATEGORY_CODE)).thenReturn(category);
        when(itemMapper.countBySecretHash(SECRET_HASH)).thenReturn(0L);
        when(cryptoService.hmacSecret(CARD_SECRET)).thenReturn(SECRET_HASH);
        when(cryptoService.encryptSecret(CARD_SECRET)).thenReturn(SECRET_CIPHER);
        when(cryptoService.mask(CARD_SECRET)).thenReturn(SECRET_MASK);
        when(cryptoService.checksum(CARD_SECRET)).thenReturn(SECRET_CHECKSUM);
        when(cryptoService.sha256(CARD_SECRET)).thenReturn(IMPORT_HASH);
        when(batchMapper.findById(BATCH_ID)).thenReturn(persistedBatch);
        when(extractLinkService.createBatchLinks(eq(BATCH_ID), any(CredentialExtractLinkCreateRequest.class)))
                .thenReturn(new CredentialExtractLinkCreateResultVo());
        doAnswer(invocation -> {
            CredentialImportTaskEntity task = invocation.getArgument(0);
            task.setId(TASK_ID);
            return 1;
        }).when(importTaskMapper).insert(any(CredentialImportTaskEntity.class));
        doAnswer(invocation -> {
            CredentialBatchEntity batch = invocation.getArgument(0);
            batch.setId(BATCH_ID);
            return 1;
        }).when(batchMapper).insert(any(CredentialBatchEntity.class));
        doAnswer(invocation -> {
            CredentialItemEntity item = invocation.getArgument(0);
            item.setId(ITEM_ID);
            return 1;
        }).when(itemMapper).insert(any(CredentialItemEntity.class));

        service.confirmImport(defaultLinkImportRequest());

        ArgumentCaptor<CredentialExtractLinkCreateRequest> requestCaptor = ArgumentCaptor.forClass(CredentialExtractLinkCreateRequest.class);
        verify(extractLinkService).createBatchLinks(eq(BATCH_ID), requestCaptor.capture());
        assertNull(requestCaptor.getValue().getExpireAt());
    }

    /**
     * 构造启用文本导入的卡密分类。
     */
    private CredentialCategoryEntity textSecretCategory() {
        CredentialCategoryEntity entity = new CredentialCategoryEntity();
        entity.setId(CATEGORY_ID);
        entity.setCategoryCode(DEFAULT_CATEGORY_CODE);
        entity.setCategoryName(CATEGORY_NAME);
        entity.setFulfillmentType(CredentialConstants.FULFILLMENT_TYPE_TEXT_SECRET);
        entity.setGenerationMode(CredentialConstants.GENERATION_MODE_TEXT_IMPORTED);
        entity.setStatus(CredentialConstants.STATUS_ACTIVE);
        return entity;
    }

    /**
     * 构造导入完成后可被查询回填的批次实体。
     */
    private CredentialBatchEntity persistedBatch() {
        CredentialBatchEntity entity = new CredentialBatchEntity();
        entity.setId(BATCH_ID);
        entity.setBatchNo("CB202605020001");
        entity.setBatchName("卡密批次-202605020001");
        entity.setCategoryId(CATEGORY_ID);
        entity.setCategoryCode(DEFAULT_CATEGORY_CODE);
        entity.setCategoryName(CATEGORY_NAME);
        entity.setFulfillmentType(CredentialConstants.FULFILLMENT_TYPE_TEXT_SECRET);
        entity.setGenerationMode(CredentialConstants.GENERATION_MODE_TEXT_IMPORTED);
        entity.setPayloadConfig("{\"label\":\"卡密\",\"remark\":\"\"}");
        entity.setTotalCount(1);
        entity.setAvailableCount(1);
        entity.setConsumedCount(0);
        entity.setLinkedCount(0);
        entity.setValidFrom("2026-05-02 10:00:00");
        entity.setValidTo("2099-12-31 23:59:59");
        entity.setStatus(CredentialConstants.STATUS_ACTIVE);
        entity.setCreatedBy(USER_ID);
        return entity;
    }

    /**
     * 构造导入后自动生成链接且未填写链接过期时间的确认请求。
     */
    private CredentialImportConfirmRequest defaultLinkImportRequest() {
        CredentialImportConfirmRequest request = new CredentialImportConfirmRequest();
        request.setRawText(CARD_SECRET);
        request.setDelimiter(NEWLINE_DELIMITER);
        request.setTrimBlank(true);
        request.setBatchDeduplicate(true);
        request.setGlobalDeduplicate(true);
        request.setCaseSensitive(true);
        request.setCreateExtractLinks(true);
        request.setItemsPerLink(1);
        request.setMaxAccessCount(3);
        return request;
    }
}
