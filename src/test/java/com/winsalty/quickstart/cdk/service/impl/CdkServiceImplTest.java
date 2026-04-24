package com.winsalty.quickstart.cdk.service.impl;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.benefit.service.BenefitGrantService;
import com.winsalty.quickstart.cdk.config.CdkProperties;
import com.winsalty.quickstart.cdk.constant.CdkConstants;
import com.winsalty.quickstart.cdk.dto.CdkExportRequest;
import com.winsalty.quickstart.cdk.entity.CdkBatchEntity;
import com.winsalty.quickstart.cdk.mapper.CdkBatchMapper;
import com.winsalty.quickstart.cdk.mapper.CdkCodeMapper;
import com.winsalty.quickstart.cdk.mapper.CdkExportAuditMapper;
import com.winsalty.quickstart.cdk.mapper.CdkRedeemRecordMapper;
import com.winsalty.quickstart.cdk.vo.CdkBatchVo;
import com.winsalty.quickstart.cdk.vo.CdkExportVo;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.infra.cache.RedisCacheService;
import com.winsalty.quickstart.infra.outbox.TransactionOutboxService;
import com.winsalty.quickstart.points.mapper.PointRechargeOrderMapper;
import com.winsalty.quickstart.risk.service.RiskAlertService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CDK 服务测试。
 * 覆盖高价值批次双人复核、二次复核人限制和加密导出审计，防止安全流程回退。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
class CdkServiceImplTest {

    private static final long ADMIN_USER_ID = 1L;
    private static final long BATCH_ID = 10L;
    private static final String ADMIN_USERNAME = "admin";
    private static final String OTHER_ADMIN_USERNAME = "other-admin";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String SESSION_ID = "session-1";
    private static final String BATCH_NO = "CB202604250001";
    private static final String BENEFIT_CONFIG = "{\"points\":1000}";
    private static final String PLAIN_CODES = "WSA-202604-AAAA-BBBB-CCCC-DDDD-A\nWSA-202604-EEEE-FFFF-GGGG-HHHH-B";
    private static final String EXPORT_PASSWORD = "Export@20260425";
    private static final String ZIP_MANIFEST_NAME = "manifest.json";
    private static final String ZIP_PAYLOAD_NAME = "cdk.enc";

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    void approveHighValueBatchShouldWaitForSecondReviewAndCreateRiskAlert() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkCodeMapper codeMapper = mock(CdkCodeMapper.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        RiskAlertService riskAlertService = mock(RiskAlertService.class);
        CdkServiceImpl service = newService(batchMapper, codeMapper, redisCacheService, riskAlertService);
        CdkBatchEntity pendingBatch = pendingBatch();
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(pendingBatch);
        when(batchMapper.findById(BATCH_ID)).thenReturn(firstApprovedBatch());
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));

        CdkBatchVo result = service.approveBatch(BATCH_ID);

        assertEquals(CdkConstants.BATCH_STATUS_PENDING_APPROVAL, result.getStatus());
        assertEquals(ADMIN_USERNAME, result.getApprovedBy());
        verify(batchMapper).markFirstApproved(BATCH_ID, ADMIN_USERNAME);
        verify(riskAlertService).createAlert(eq(CdkConstants.ALERT_TYPE_BATCH_DOUBLE_REVIEW),
                eq(CdkConstants.RISK_LEVEL_HIGH), eq(CdkConstants.SUBJECT_TYPE_CDK_BATCH),
                eq(BATCH_NO), eq(null), anyString());
        verify(codeMapper, never()).insert(any());
        verify(redisCacheService, never()).set(anyString(), any(), anyLong());
    }

    @Test
    void secondApproveShouldRejectOriginalApprover() {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        CdkServiceImpl service = newService(batchMapper, mock(CdkCodeMapper.class), mock(RedisCacheService.class), mock(RiskAlertService.class));
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(firstApprovedBatch());
        AuthContext.set(new AuthUser(ADMIN_USER_ID, ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));

        assertThrows(BusinessException.class, () -> service.secondApproveBatch(BATCH_ID));
    }

    @Test
    void exportBatchShouldReturnEncryptedZipAndConsumePlaintextCache() throws Exception {
        CdkBatchMapper batchMapper = mock(CdkBatchMapper.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        CdkExportAuditMapper exportAuditMapper = mock(CdkExportAuditMapper.class);
        CdkServiceImpl service = newService(batchMapper, mock(CdkCodeMapper.class), redisCacheService, mock(RiskAlertService.class), exportAuditMapper);
        when(batchMapper.findByIdForUpdate(BATCH_ID)).thenReturn(activeBatch());
        when(redisCacheService.get(CdkConstants.EXPORT_CACHE_PREFIX + BATCH_NO)).thenReturn(PLAIN_CODES);
        AuthContext.set(new AuthUser(ADMIN_USER_ID, OTHER_ADMIN_USERNAME, ROLE_ADMIN, SESSION_ID));

        CdkExportVo result = service.exportBatch(BATCH_ID, exportRequest());

        assertEquals(BATCH_NO, result.getBatchNo());
        assertEquals(Integer.valueOf(2), result.getCount());
        assertEquals("zip", result.getFileType());
        assertNotNull(result.getFingerprint());
        assertNotNull(result.getEncryptedPackageBase64());
        assertFalse(result.getEncryptedPackageBase64().contains("WSA-202604"));
        assertZipContainsManifestAndEncryptedPayload(result.getEncryptedPackageBase64());
        verify(redisCacheService).delete(CdkConstants.EXPORT_CACHE_PREFIX + BATCH_NO);
        verify(exportAuditMapper).insert(any());
        verify(batchMapper).incrementExport(BATCH_ID);
    }

    private CdkServiceImpl newService(CdkBatchMapper batchMapper,
                                      CdkCodeMapper codeMapper,
                                      RedisCacheService redisCacheService,
                                      RiskAlertService riskAlertService) {
        return newService(batchMapper, codeMapper, redisCacheService, riskAlertService, mock(CdkExportAuditMapper.class));
    }

    private CdkServiceImpl newService(CdkBatchMapper batchMapper,
                                      CdkCodeMapper codeMapper,
                                      RedisCacheService redisCacheService,
                                      RiskAlertService riskAlertService,
                                      CdkExportAuditMapper exportAuditMapper) {
        CdkProperties properties = new CdkProperties();
        properties.setPepper("0123456789abcdef0123456789abcdef");
        properties.setDoubleReviewEnabled(true);
        properties.setDoubleReviewTotalPointsThreshold(1000L);
        properties.setExportEncryptIterations(1000);
        return new CdkServiceImpl(
                batchMapper,
                codeMapper,
                mock(CdkRedeemRecordMapper.class),
                exportAuditMapper,
                mock(PointRechargeOrderMapper.class),
                mock(BenefitGrantService.class),
                redisCacheService,
                mock(TransactionOutboxService.class),
                riskAlertService,
                properties
        );
    }

    private CdkExportRequest exportRequest() {
        CdkExportRequest request = new CdkExportRequest();
        request.setExportPassword(EXPORT_PASSWORD);
        return request;
    }

    private CdkBatchEntity pendingBatch() {
        CdkBatchEntity entity = activeBatch();
        entity.setStatus(CdkConstants.BATCH_STATUS_PENDING_APPROVAL);
        entity.setRiskLevel(CdkConstants.RISK_LEVEL_HIGH);
        entity.setApprovedBy(null);
        return entity;
    }

    private CdkBatchEntity firstApprovedBatch() {
        CdkBatchEntity entity = pendingBatch();
        entity.setApprovedBy(ADMIN_USERNAME);
        return entity;
    }

    private CdkBatchEntity activeBatch() {
        CdkBatchEntity entity = new CdkBatchEntity();
        entity.setId(BATCH_ID);
        entity.setBatchNo(BATCH_NO);
        entity.setBatchName("高价值批次");
        entity.setBenefitType(CdkConstants.BENEFIT_TYPE_POINTS);
        entity.setBenefitConfig(BENEFIT_CONFIG);
        entity.setTotalCount(1);
        entity.setGeneratedCount(0);
        entity.setRedeemedCount(0);
        entity.setValidFrom("2026-04-24 00:00:00");
        entity.setValidTo("2026-04-26 00:00:00");
        entity.setStatus(CdkConstants.BATCH_STATUS_ACTIVE);
        entity.setRiskLevel(CdkConstants.RISK_LEVEL_NORMAL);
        entity.setCreatedBy(ADMIN_USERNAME);
        entity.setExportCount(0);
        return entity;
    }

    private void assertZipContainsManifestAndEncryptedPayload(String encryptedPackageBase64) throws Exception {
        byte[] zipBytes = Base64.getDecoder().decode(encryptedPackageBase64);
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        boolean hasManifest = false;
        boolean hasPayload = false;
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            byte[] bytes = readEntry(zipInputStream);
            if (ZIP_MANIFEST_NAME.equals(entry.getName())) {
                hasManifest = new String(bytes, StandardCharsets.UTF_8).contains("\"algorithm\":\"AES-256-GCM\"");
            }
            if (ZIP_PAYLOAD_NAME.equals(entry.getName())) {
                hasPayload = bytes.length > 0;
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();
        assertTrue(hasManifest);
        assertTrue(hasPayload);
    }

    private byte[] readEntry(ZipInputStream zipInputStream) throws Exception {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = zipInputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }
}
