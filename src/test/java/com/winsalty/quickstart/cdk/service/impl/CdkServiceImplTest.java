package com.winsalty.quickstart.cdk.service.impl;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.benefit.service.BenefitGrantService;
import com.winsalty.quickstart.cdk.config.CdkProperties;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.cdk.dto.CdkBatchCreateRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeListRequest;
import com.winsalty.quickstart.cdk.dto.CdkCodeStatusRequest;
import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import com.winsalty.quickstart.cdk.entity.CdkCodeEntity;
import com.winsalty.quickstart.cdk.mapper.CdkBatchMapper;
import com.winsalty.quickstart.cdk.mapper.CdkCodeMapper;
import com.winsalty.quickstart.cdk.mapper.CdkRedeemRecordMapper;
import com.winsalty.quickstart.cdk.vo.CdkCodeVo;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxService;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.risk.service.RiskAlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CDK 服务测试。
 * 覆盖管理员直接生成、在线查看明文和已兑换码状态保护。
 * 创建日期：2026-04-26
 * author：sunshengxian
 */
class CdkServiceImplTest {

    private static final long ADMIN_USER_ID = 1L;
    private static final long BATCH_ID = 10L;
    private static final long CODE_ID = 20L;
    private static final String ADMIN_USERNAME = "admin";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String SESSION_ID = "session-1";
    private static final String BATCH_NO = "CB202604260001";
    private static final String BENEFIT_CONFIG = "{\"points\":1000}";
    private static final int SHORT_CDK_PART_COUNT = 6;

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    void createBatchShouldGenerateActiveCodesImmediately() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, redisCacheService);
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));
        doAnswer(invocation -> {
            CdkBatchEntity entity = invocation.getArgument(0);
            entity.setId(BATCH_ID);
            entity.setBatchNo(BATCH_NO);
            return 1;
        }).when(batchMapper).insert(any(CdkBatchEntity.class));
        when(batchMapper.findById(BATCH_ID)).thenReturn(activeBatch());

        service.createBatch(batchRequest());

        ArgumentCaptor<CdkCodeEntity> codeCaptor = ArgumentCaptor.forClass(CdkCodeEntity.class);
        verify(codeMapper).insert(codeCaptor.capture());
        CdkCodeEntity generatedCode = codeCaptor.getValue();
        assertEquals(CdkConstants.CODE_STATUS_ACTIVE, generatedCode.getStatus());
        assertNotNull(generatedCode.getEncryptedCode());
        assertFalse(generatedCode.getEncryptedCode().startsWith(CdkConstants.CODE_PREFIX));
        verify(batchMapper).markGenerated(BATCH_ID, 1, CdkConstants.BATCH_STATUS_ACTIVE);
        verify(redisCacheService, never()).set(any(), any(), anyLong());
    }

    @Test
    void listCodesShouldReturnPlainTextAndBatchInfo() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, mock(RedisCacheService.class));
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));
        CdkCodeEntity generatedCode = generatedCodeFromCreate(service, batchMapper, codeMapper);
        generatedCode.setBatchNo(BATCH_NO);
        generatedCode.setBatchName("运营发放");
        generatedCode.setBenefitType(CdkConstants.BENEFIT_TYPE_POINTS);
        generatedCode.setBenefitConfig(BENEFIT_CONFIG);
        generatedCode.setBatchStatus(CdkConstants.BATCH_STATUS_ACTIVE);
        generatedCode.setValidFrom("2026-04-26 00:00:00");
        generatedCode.setValidTo("2026-05-26 00:00:00");
        when(batchMapper.findById(BATCH_ID)).thenReturn(activeBatch());
        when(codeMapper.findPage(null, BATCH_ID, null, 0, 10)).thenReturn(Collections.singletonList(generatedCode));
        when(codeMapper.countPage(null, BATCH_ID, null)).thenReturn(1L);

        CdkCodeListRequest request = new CdkCodeListRequest();
        request.setBatchId(BATCH_ID);
        PageResponse<CdkCodeVo> page = service.listCodes(request);
        CdkCodeVo code = page.getRecords().get(0);

        assertEquals(1L, page.getTotal());
        assertTrue(code.getCdk().startsWith(CdkConstants.CODE_PREFIX));
        assertEquals(SHORT_CDK_PART_COUNT, code.getCdk().split("-").length);
        assertEquals(BATCH_NO, code.getBatchNo());
        assertEquals(BENEFIT_CONFIG, code.getBenefitConfig());
    }

    @Test
    void updateRedeemedCodeStatusShouldBeRejected() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, mock(RedisCacheService.class));
        CdkCodeEntity code = activeCode();
        code.setStatus(CdkConstants.CODE_STATUS_REDEEMED);
        when(codeMapper.findByIdForUpdate(CODE_ID)).thenReturn(code);
        CdkCodeStatusRequest request = new CdkCodeStatusRequest();
        request.setStatus(CdkConstants.CODE_STATUS_DISABLED);

        assertThrows(BusinessException.class, () -> service.updateCodeStatus(CODE_ID, request));
    }

    @Test
    void voidBatchShouldDisableActiveCodes() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, mock(RedisCacheService.class));
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(activeBatch());
        when(batchMapper.findById(BATCH_ID)).thenReturn(voidedBatch());
        when(codeMapper.disableActiveByBatchId(BATCH_ID)).thenReturn(1);

        service.voidBatch(BATCH_ID);

        verify(codeMapper).disableActiveByBatchId(BATCH_ID);
        verify(batchMapper).updateStatus(BATCH_ID, CdkConstants.BATCH_STATUS_VOIDED);
    }

    @Test
    void enableCodeInVoidedBatchShouldBeRejected() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, mock(RedisCacheService.class));
        CdkCodeEntity code = activeCode();
        code.setStatus(CdkConstants.CODE_STATUS_DISABLED);
        when(codeMapper.findByIdForUpdate(CODE_ID)).thenReturn(code);
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(voidedBatch());
        CdkCodeStatusRequest request = new CdkCodeStatusRequest();
        request.setStatus(CdkConstants.CODE_STATUS_ACTIVE);

        assertThrows(BusinessException.class, () -> service.updateCodeStatus(CODE_ID, request));
        verify(codeMapper, never()).updateStatus(anyLong(), any());
    }

    private CdkCodeEntity generatedCodeFromCreate(CdkServiceImpl service, CdkBatchMapper batchMapper, CdkCodeMapper codeMapper) {
        doAnswer(invocation -> {
            CdkBatchEntity entity = invocation.getArgument(0);
            entity.setId(BATCH_ID);
            entity.setBatchNo(BATCH_NO);
            return 1;
        }).when(batchMapper).insert(any(CdkBatchEntity.class));
        when(batchMapper.findById(BATCH_ID)).thenReturn(activeBatch());
        service.createBatch(batchRequest());
        ArgumentCaptor<CdkCodeEntity> codeCaptor = ArgumentCaptor.forClass(CdkCodeEntity.class);
        verify(codeMapper).insert(codeCaptor.capture());
        CdkCodeEntity code = codeCaptor.getValue();
        code.setId(CODE_ID);
        code.setCreatedAt("2026-04-26 10:00:00");
        code.setUpdatedAt("2026-04-26 10:00:00");
        return code;
    }

    private CdkServiceImpl newService(CdkBatchMapper batchMapper,
                                      CdkCodeMapper codeMapper,
                                      RedisCacheService redisCacheService) {
        CdkProperties properties = new CdkProperties();
        properties.setPepper("0123456789abcdef0123456789abcdef");
        return new CdkServiceImpl(
                batchMapper,
                codeMapper,
                mock(CdkRedeemRecordMapper.class),
                mock(PointRechargeOrderMapper.class),
                mock(BenefitGrantService.class),
                redisCacheService,
                mock(TransactionOutboxService.class),
                mock(RiskAlertService.class),
                properties
        );
    }

    private CdkBatchCreateRequest batchRequest() {
        CdkBatchCreateRequest request = new CdkBatchCreateRequest();
        request.setBatchName("运营发放");
        request.setBenefitType(CdkConstants.BENEFIT_TYPE_POINTS);
        request.setPoints(1000L);
        request.setTotalCount(1);
        request.setRiskLevel(CdkConstants.RISK_LEVEL_NORMAL);
        request.setValidFrom("2026-04-26 00:00:00");
        request.setValidTo("2026-05-26 00:00:00");
        return request;
    }

    private CdkBatchEntity activeBatch() {
        CdkBatchEntity entity = new CdkBatchEntity();
        entity.setId(BATCH_ID);
        entity.setBatchNo(BATCH_NO);
        entity.setBatchName("运营发放");
        entity.setBenefitType(CdkConstants.BENEFIT_TYPE_POINTS);
        entity.setBenefitConfig(BENEFIT_CONFIG);
        entity.setTotalCount(1);
        entity.setGeneratedCount(1);
        entity.setRedeemedCount(0);
        entity.setValidFrom("2026-04-26 00:00:00");
        entity.setValidTo("2026-05-26 00:00:00");
        entity.setStatus(CdkConstants.BATCH_STATUS_ACTIVE);
        entity.setRiskLevel(CdkConstants.RISK_LEVEL_NORMAL);
        entity.setCreatedBy(ADMIN_USERNAME);
        entity.setExportCount(0);
        return entity;
    }

    private CdkBatchEntity voidedBatch() {
        CdkBatchEntity entity = activeBatch();
        entity.setStatus(CdkConstants.BATCH_STATUS_VOIDED);
        return entity;
    }

    private CdkCodeEntity activeCode() {
        CdkCodeEntity entity = new CdkCodeEntity();
        entity.setId(CODE_ID);
        entity.setBatchId(BATCH_ID);
        entity.setCodeHash("hash");
        entity.setEncryptedCode("encrypted");
        entity.setCodePrefix("WSA-202604");
        entity.setChecksum("A");
        entity.setStatus(CdkConstants.CODE_STATUS_ACTIVE);
        entity.setVersion(0L);
        return entity;
    }
}
