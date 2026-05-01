package com.winsalty.quickstart.credential.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.credential.config.CredentialProperties;
import com.winsalty.quickstart.credential.constant.CredentialConstants;
import com.winsalty.quickstart.credential.dto.CredentialBatchListRequest;
import com.winsalty.quickstart.credential.dto.CredentialCategorySaveRequest;
import com.winsalty.quickstart.credential.dto.CredentialExtractLinkCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialGeneratedBatchCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportConfirmRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportPreviewRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportTaskListRequest;
import com.winsalty.quickstart.credential.dto.CredentialItemListRequest;
import com.winsalty.quickstart.credential.dto.CredentialRedeemRecordListRequest;
import com.winsalty.quickstart.credential.entity.CredentialBatchEntity;
import com.winsalty.quickstart.credential.entity.CredentialCategoryEntity;
import com.winsalty.quickstart.credential.entity.CredentialImportTaskEntity;
import com.winsalty.quickstart.credential.entity.CredentialItemEntity;
import com.winsalty.quickstart.credential.entity.CredentialOperationAuditEntity;
import com.winsalty.quickstart.credential.entity.CredentialRedeemRecordEntity;
import com.winsalty.quickstart.credential.mapper.CredentialBatchMapper;
import com.winsalty.quickstart.credential.mapper.CredentialCategoryMapper;
import com.winsalty.quickstart.credential.mapper.CredentialImportTaskMapper;
import com.winsalty.quickstart.credential.mapper.CredentialItemMapper;
import com.winsalty.quickstart.credential.mapper.CredentialOperationAuditMapper;
import com.winsalty.quickstart.credential.mapper.CredentialRedeemRecordMapper;
import com.winsalty.quickstart.credential.service.CredentialAdminService;
import com.winsalty.quickstart.credential.service.CredentialExtractLinkService;
import com.winsalty.quickstart.credential.service.support.CredentialCryptoService;
import com.winsalty.quickstart.credential.vo.CredentialBatchVo;
import com.winsalty.quickstart.credential.vo.CredentialCategoryVo;
import com.winsalty.quickstart.credential.vo.CredentialExtractLinkCreateResultVo;
import com.winsalty.quickstart.credential.vo.CredentialGeneratedSecretVo;
import com.winsalty.quickstart.credential.vo.CredentialImportPreviewItemVo;
import com.winsalty.quickstart.credential.vo.CredentialImportPreviewVo;
import com.winsalty.quickstart.credential.vo.CredentialImportTaskVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;
import com.winsalty.quickstart.credential.vo.CredentialRedeemRecordVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端凭证中心服务实现。
 * 负责凭证分类、批次、明细、卡密导入、导入任务和兑换记录后台管理。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
@Service
public class CredentialAdminServiceImpl extends BaseService implements CredentialAdminService {

    private static final Logger log = LoggerFactory.getLogger(CredentialAdminServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String BATCH_NO_PREFIX = "CB";
    private static final String ITEM_NO_PREFIX = "CI";
    private static final String TASK_NO_PREFIX = "CIT";
    private static final String AUDIT_NO_PREFIX = "COA";
    private static final String OPERATION_REVEAL = "item_reveal";
    private static final String TARGET_TYPE_ITEM = "item";
    private static final String IMPORT_STATUS_PREVIEWED = "previewed";
    private static final String IMPORT_STATUS_SUCCESS = "success";
    private static final String PREVIEW_STATUS_VALID = "valid";
    private static final String PREVIEW_STATUS_DUPLICATE = "duplicate";
    private static final String PREVIEW_STATUS_INVALID = "invalid";
    private static final String NEWLINE_DELIMITER = "\\n";
    private static final String TAB_DELIMITER = "\\t";
    private static final String DEFAULT_POINTS_BATCH_NAME_PREFIX = "积分CDK批次-";
    private static final String DEFAULT_TEXT_SECRET_BATCH_NAME_PREFIX = "卡密批次-";
    private static final String DEFAULT_PERMANENT_EXPIRE_AT = "2099-12-31 23:59:59";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PREVIEW_LIMIT = 50;
    private static final int UUID_FRAGMENT_LENGTH = 12;

    private final CredentialCategoryMapper credentialCategoryMapper;
    private final CredentialBatchMapper credentialBatchMapper;
    private final CredentialItemMapper credentialItemMapper;
    private final CredentialImportTaskMapper credentialImportTaskMapper;
    private final CredentialRedeemRecordMapper credentialRedeemRecordMapper;
    private final CredentialOperationAuditMapper credentialOperationAuditMapper;
    private final CredentialExtractLinkService credentialExtractLinkService;
    private final CredentialCryptoService credentialCryptoService;
    private final CredentialProperties credentialProperties;

    public CredentialAdminServiceImpl(CredentialCategoryMapper credentialCategoryMapper,
                                      CredentialBatchMapper credentialBatchMapper,
                                      CredentialItemMapper credentialItemMapper,
                                      CredentialImportTaskMapper credentialImportTaskMapper,
                                      CredentialRedeemRecordMapper credentialRedeemRecordMapper,
                                      CredentialOperationAuditMapper credentialOperationAuditMapper,
                                      CredentialExtractLinkService credentialExtractLinkService,
                                      CredentialCryptoService credentialCryptoService,
                                      CredentialProperties credentialProperties) {
        this.credentialCategoryMapper = credentialCategoryMapper;
        this.credentialBatchMapper = credentialBatchMapper;
        this.credentialItemMapper = credentialItemMapper;
        this.credentialImportTaskMapper = credentialImportTaskMapper;
        this.credentialRedeemRecordMapper = credentialRedeemRecordMapper;
        this.credentialOperationAuditMapper = credentialOperationAuditMapper;
        this.credentialExtractLinkService = credentialExtractLinkService;
        this.credentialCryptoService = credentialCryptoService;
        this.credentialProperties = credentialProperties;
    }

    /**
     * 查询凭证分类。
     */
    @Override
    public List<CredentialCategoryVo> listCategories() {
        return toCategoryVos(credentialCategoryMapper.findAll());
    }

    /**
     * 创建凭证分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialCategoryVo createCategory(CredentialCategorySaveRequest request) {
        CredentialCategoryEntity entity = fillCategory(new CredentialCategoryEntity(), request);
        credentialCategoryMapper.insert(entity);
        log.info("credential category created, categoryCode={}, operator={}", entity.getCategoryCode(), currentUserId());
        return toCategoryVo(credentialCategoryMapper.findById(entity.getId()));
    }

    /**
     * 更新凭证分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialCategoryVo updateCategory(Long id, CredentialCategorySaveRequest request) {
        CredentialCategoryEntity existed = loadCategory(id);
        fillCategory(existed, request);
        credentialCategoryMapper.update(existed);
        log.info("credential category updated, categoryId={}, operator={}", id, currentUserId());
        return toCategoryVo(credentialCategoryMapper.findById(id));
    }

    /**
     * 停用凭证分类。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialCategoryVo disableCategory(Long id) {
        loadCategory(id);
        credentialCategoryMapper.disable(id);
        log.info("credential category disabled, categoryId={}, operator={}", id, currentUserId());
        return toCategoryVo(credentialCategoryMapper.findById(id));
    }

    /**
     * 分页查询凭证批次。
     */
    @Override
    public PageResponse<CredentialBatchVo> listBatches(CredentialBatchListRequest request) {
        CredentialBatchListRequest query = request == null ? new CredentialBatchListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        List<CredentialBatchEntity> entities = credentialBatchMapper.findPage(query.getKeyword(), query.getCategoryId(),
                query.getFulfillmentType(), query.getGenerationMode(), query.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = credentialBatchMapper.countPage(query.getKeyword(), query.getCategoryId(),
                query.getFulfillmentType(), query.getGenerationMode(), query.getStatus());
        return new PageResponse<CredentialBatchVo>(toBatchVos(entities), pageNo, pageSize, total);
    }

    /**
     * 查询凭证批次详情。
     */
    @Override
    public CredentialBatchVo getBatch(Long id) {
        return toBatchVo(loadBatch(id));
    }

    /**
     * 创建系统生成凭证批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialBatchVo createGeneratedBatch(CredentialGeneratedBatchCreateRequest request) {
        if (request.getTotalCount() > credentialProperties.getMaxBatchSize()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_SIZE_EXCEEDED);
        }
        CredentialCategoryEntity category = resolveCategory(request.getCategoryId(), CredentialConstants.CATEGORY_POINTS_CDK);
        if (!CredentialConstants.GENERATION_MODE_SYSTEM_GENERATED.equals(category.getGenerationMode())
                && !"MIXED".equals(category.getGenerationMode())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_STATUS_INVALID, "分类不支持系统生成");
        }
        JSONObject payload = new JSONObject();
        payload.put("points", request.getPoints());
        payload.put("remark", request.getRemark());
        CredentialBatchEntity batch = buildBatch(defaultBatchName(request.getBatchName(), DEFAULT_POINTS_BATCH_NAME_PREFIX), category, CredentialConstants.GENERATION_MODE_SYSTEM_GENERATED,
                payload.toJSONString(), request.getTotalCount(), request.getValidFrom(), request.getValidTo());
        credentialBatchMapper.insert(batch);
        List<CredentialGeneratedSecretVo> generatedSecrets = new ArrayList<CredentialGeneratedSecretVo>();
        for (int index = 0; index < request.getTotalCount(); index++) {
            String secretText = createGeneratedSecret();
            CredentialItemEntity item = insertItem(batch, secretText, CredentialConstants.SOURCE_TYPE_GENERATED, null);
            CredentialGeneratedSecretVo secretVo = new CredentialGeneratedSecretVo();
            secretVo.setItemNo(item.getItemNo());
            secretVo.setSecretText(secretText);
            secretVo.setCopyLabel("CDK " + (index + 1));
            generatedSecrets.add(secretVo);
        }
        log.info("credential generated batch created, batchNo={}, totalCount={}, operator={}",
                batch.getBatchNo(), request.getTotalCount(), currentUserId());
        CredentialBatchVo vo = toBatchVo(credentialBatchMapper.findById(batch.getId()));
        vo.setGeneratedSecrets(generatedSecrets);
        return vo;
    }

    /**
     * 预览文本卡密导入结果。
     */
    @Override
    public CredentialImportPreviewVo previewImport(CredentialImportPreviewRequest request) {
        resolveCategory(request.getCategoryId(), CredentialConstants.CATEGORY_TEXT_CARD_SECRET);
        ImportParseResult result = parseImport(request);
        log.info("credential import preview parsed, categoryId={}, totalRows={}, validRows={}",
                request.getCategoryId(), result.totalRows, result.validRows);
        return result.toVo();
    }

    /**
     * 确认导入文本卡密批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialBatchVo confirmImport(CredentialImportConfirmRequest request) {
        CredentialCategoryEntity category = resolveCategory(request.getCategoryId(), CredentialConstants.CATEGORY_TEXT_CARD_SECRET);
        if (!CredentialConstants.GENERATION_MODE_TEXT_IMPORTED.equals(category.getGenerationMode())
                && !"MIXED".equals(category.getGenerationMode())) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_STATUS_INVALID, "分类不支持文本导入");
        }
        ImportParseResult result = parseImport(request);
        if (result.validRows <= 0) {
            throw new BusinessException(ErrorCode.CREDENTIAL_SECRET_INVALID, "没有可导入的有效卡密");
        }
        CredentialImportTaskEntity task = buildImportTask(request, category, result, IMPORT_STATUS_PREVIEWED);
        credentialImportTaskMapper.insert(task);
        CredentialBatchEntity batch = buildBatch(defaultBatchName(request.getBatchName(), DEFAULT_TEXT_SECRET_BATCH_NAME_PREFIX), category, CredentialConstants.GENERATION_MODE_TEXT_IMPORTED,
                "{\"label\":\"卡密\",\"remark\":\"" + safeJsonText(request.getRemark()) + "\"}", result.validRows,
                request.getValidFrom(), request.getValidTo());
        credentialBatchMapper.insert(batch);
        for (ImportSecret secret : result.validSecrets) {
            insertItem(batch, secret.secretText, CredentialConstants.SOURCE_TYPE_IMPORTED, secret.lineNo);
        }
        task.setBatchId(batch.getId());
        task.setStatus(IMPORT_STATUS_SUCCESS);
        task.setResultSummary(JSON.toJSONString(result.toVo()));
        credentialImportTaskMapper.updateResult(task);
        CredentialExtractLinkCreateResultVo linkResult = null;
        if (Boolean.TRUE.equals(request.getCreateExtractLinks())) {
            CredentialExtractLinkCreateRequest linkRequest = new CredentialExtractLinkCreateRequest();
            linkRequest.setItemsPerLink(request.getItemsPerLink());
            linkRequest.setMaxAccessCount(request.getMaxAccessCount());
            linkRequest.setExpireAt(defaultExpireDateTime(request.getExpireAt()));
            linkRequest.setRemark(request.getRemark());
            linkResult = credentialExtractLinkService.createBatchLinks(batch.getId(), linkRequest);
        }
        log.info("credential imported batch created, batchNo={}, validRows={}, operator={}",
                batch.getBatchNo(), result.validRows, currentUserId());
        CredentialBatchVo vo = toBatchVo(credentialBatchMapper.findById(batch.getId()));
        vo.setExtractLinks(linkResult == null ? null : linkResult.getLinks());
        return vo;
    }

    /**
     * 停用凭证批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialBatchVo disableBatch(Long id) {
        loadBatch(id);
        credentialBatchMapper.disable(id);
        log.info("credential batch disabled, batchId={}, operator={}", id, currentUserId());
        return toBatchVo(credentialBatchMapper.findById(id));
    }

    /**
     * 分页查询凭证明细。
     */
    @Override
    public PageResponse<CredentialItemVo> listItems(CredentialItemListRequest request) {
        CredentialItemListRequest query = request == null ? new CredentialItemListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        List<CredentialItemEntity> entities = credentialItemMapper.findPage(query.getKeyword(), query.getBatchId(),
                query.getCategoryId(), query.getSourceType(), query.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = credentialItemMapper.countPage(query.getKeyword(), query.getBatchId(), query.getCategoryId(),
                query.getSourceType(), query.getStatus());
        return new PageResponse<CredentialItemVo>(toItemVos(entities), pageNo, pageSize, total);
    }

    /**
     * 查看凭证明文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String revealItem(Long id, HttpServletRequest servletRequest) {
        CredentialItemEntity item = loadItem(id);
        String secretText = credentialCryptoService.decryptSecret(item.getEncryptedSecret());
        CredentialOperationAuditEntity audit = new CredentialOperationAuditEntity();
        audit.setAuditNo(createNo(AUDIT_NO_PREFIX));
        audit.setOperatorId(currentUserId());
        audit.setOperationType(OPERATION_REVEAL);
        audit.setTargetType(TARGET_TYPE_ITEM);
        audit.setTargetId(id);
        audit.setBeforeSnapshot(JSON.toJSONString(toItemVo(item)));
        audit.setAfterSnapshot(null);
        audit.setClientIp(IpUtils.getClientIp(servletRequest));
        audit.setSuccess(1);
        audit.setFailureReason("");
        credentialOperationAuditMapper.insert(audit);
        log.info("credential item revealed, itemNo={}, operator={}", item.getItemNo(), currentUserId());
        return secretText;
    }

    /**
     * 停用凭证明细。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialItemVo disableItem(Long id) {
        loadItem(id);
        credentialItemMapper.disable(id);
        log.info("credential item disabled, itemId={}, operator={}", id, currentUserId());
        return toItemVo(credentialItemMapper.findById(id));
    }

    /**
     * 启用凭证明细。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CredentialItemVo enableItem(Long id) {
        loadItem(id);
        credentialItemMapper.enable(id);
        log.info("credential item enabled, itemId={}, operator={}", id, currentUserId());
        return toItemVo(credentialItemMapper.findById(id));
    }

    /**
     * 分页查询导入任务。
     */
    @Override
    public PageResponse<CredentialImportTaskVo> listImportTasks(CredentialImportTaskListRequest request) {
        CredentialImportTaskListRequest query = request == null ? new CredentialImportTaskListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        List<CredentialImportTaskEntity> entities = credentialImportTaskMapper.findPage(query.getCategoryId(),
                query.getBatchId(), query.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = credentialImportTaskMapper.countPage(query.getCategoryId(), query.getBatchId(), query.getStatus());
        return new PageResponse<CredentialImportTaskVo>(toImportTaskVos(entities), pageNo, pageSize, total);
    }

    /**
     * 分页查询兑换记录。
     */
    @Override
    public PageResponse<CredentialRedeemRecordVo> listRedeemRecords(CredentialRedeemRecordListRequest request) {
        CredentialRedeemRecordListRequest query = request == null ? new CredentialRedeemRecordListRequest() : request;
        int pageNo = normalizePageNo(query.getPageNo());
        int pageSize = normalizePageSize(query.getPageSize());
        List<CredentialRedeemRecordEntity> entities = credentialRedeemRecordMapper.findPage(query.getUserId(),
                query.getBatchId(), query.getStatus(), offset(pageNo, pageSize), pageSize);
        long total = credentialRedeemRecordMapper.countPage(query.getUserId(), query.getBatchId(), query.getStatus());
        return new PageResponse<CredentialRedeemRecordVo>(toRedeemRecordVos(entities), pageNo, pageSize, total);
    }

    private CredentialCategoryEntity fillCategory(CredentialCategoryEntity entity, CredentialCategorySaveRequest request) {
        entity.setCategoryCode(request.getCategoryCode().trim());
        entity.setCategoryName(request.getCategoryName().trim());
        entity.setFulfillmentType(request.getFulfillmentType().trim());
        entity.setGenerationMode(request.getGenerationMode().trim());
        entity.setPayloadSchema(request.getPayloadSchema());
        entity.setImportConfig(request.getImportConfig());
        entity.setExtractPolicy(request.getExtractPolicy());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : CredentialConstants.STATUS_ACTIVE);
        return entity;
    }

    private CredentialBatchEntity buildBatch(String batchName, CredentialCategoryEntity category, String generationMode,
                                             String payloadConfig, int totalCount, String validFrom, String validTo) {
        CredentialBatchEntity batch = new CredentialBatchEntity();
        batch.setBatchNo(createNo(BATCH_NO_PREFIX));
        batch.setBatchName(batchName);
        batch.setCategoryId(category.getId());
        batch.setFulfillmentType(category.getFulfillmentType());
        batch.setGenerationMode(generationMode);
        batch.setPayloadConfig(payloadConfig);
        batch.setTotalCount(totalCount);
        batch.setAvailableCount(totalCount);
        batch.setConsumedCount(0);
        batch.setLinkedCount(0);
        batch.setValidFrom(defaultStartDateTime(validFrom));
        batch.setValidTo(defaultExpireDateTime(validTo));
        batch.setStatus(CredentialConstants.STATUS_ACTIVE);
        batch.setCreatedBy(currentUserId());
        return batch;
    }

    private CredentialItemEntity insertItem(CredentialBatchEntity batch, String secretText, String sourceType, Integer lineNo) {
        CredentialItemEntity item = new CredentialItemEntity();
        item.setBatchId(batch.getId());
        item.setCategoryId(batch.getCategoryId());
        item.setItemNo(createNo(ITEM_NO_PREFIX));
        item.setSecretHash(credentialCryptoService.hmacSecret(secretText));
        item.setEncryptedSecret(credentialCryptoService.encryptSecret(secretText));
        item.setSecretMask(credentialCryptoService.mask(secretText));
        item.setChecksum(credentialCryptoService.checksum(secretText));
        item.setPayloadSnapshot(batch.getPayloadConfig());
        item.setSourceType(sourceType);
        item.setSourceLineNo(lineNo);
        item.setStatus(CredentialConstants.STATUS_ACTIVE);
        credentialItemMapper.insert(item);
        return item;
    }

    private ImportParseResult parseImport(CredentialImportPreviewRequest request) {
        String rawText = request.getRawText();
        if (rawText.getBytes(StandardCharsets.UTF_8).length > credentialProperties.getImportMaxTextBytes()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_SIZE_EXCEEDED, "导入文本超过大小限制");
        }
        String delimiter = resolveDelimiter(request.getDelimiter());
        String[] parts = rawText.split(java.util.regex.Pattern.quote(delimiter), -1);
        if (parts.length > credentialProperties.getImportMaxItems()) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_SIZE_EXCEEDED);
        }
        ImportParseResult result = new ImportParseResult();
        result.importHash = credentialCryptoService.sha256(rawText);
        Set<String> batchHashes = new HashSet<String>();
        for (int index = 0; index < parts.length; index++) {
            String value = Boolean.FALSE.equals(request.getTrimBlank()) ? parts[index] : parts[index].trim();
            int lineNo = index + 1;
            result.totalRows++;
            if (!StringUtils.hasText(value)) {
                result.addPreview(lineNo, "", PREVIEW_STATUS_INVALID, "空行");
                continue;
            }
            String normalized = Boolean.FALSE.equals(request.getCaseSensitive()) ? value.toLowerCase() : value;
            String hash = credentialCryptoService.hmacSecret(normalized);
            if (Boolean.TRUE.equals(request.getBatchDeduplicate()) && batchHashes.contains(hash)) {
                result.addPreview(lineNo, credentialCryptoService.mask(value), PREVIEW_STATUS_DUPLICATE, "批内重复");
                continue;
            }
            if (Boolean.TRUE.equals(request.getGlobalDeduplicate()) && credentialItemMapper.countBySecretHash(hash) > 0) {
                result.addPreview(lineNo, credentialCryptoService.mask(value), PREVIEW_STATUS_DUPLICATE, "全库重复");
                continue;
            }
            batchHashes.add(hash);
            result.validRows++;
            result.validSecrets.add(new ImportSecret(lineNo, normalized));
            result.addPreview(lineNo, credentialCryptoService.mask(value), PREVIEW_STATUS_VALID, "有效");
        }
        return result;
    }

    private CredentialImportTaskEntity buildImportTask(CredentialImportConfirmRequest request,
                                                       CredentialCategoryEntity category,
                                                       ImportParseResult result,
                                                       String status) {
        CredentialImportTaskEntity entity = new CredentialImportTaskEntity();
        entity.setTaskNo(createNo(TASK_NO_PREFIX));
        entity.setCategoryId(category.getId());
        entity.setDelimiter(request.getDelimiter());
        entity.setTotalRows(result.totalRows);
        entity.setValidRows(result.validRows);
        entity.setDuplicateRows(result.duplicateRows);
        entity.setInvalidRows(result.invalidRows);
        entity.setImportHash(result.importHash);
        entity.setResultSummary(JSON.toJSONString(result.toVo()));
        entity.setStatus(status);
        entity.setCreatedBy(currentUserId());
        return entity;
    }

    private String resolveDelimiter(String delimiter) {
        if (!StringUtils.hasText(delimiter) || NEWLINE_DELIMITER.equals(delimiter)) {
            return "\n";
        }
        if (TAB_DELIMITER.equals(delimiter)) {
            return "\t";
        }
        if (delimiter.length() > 8) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "自定义分隔符最长 8 个字符");
        }
        return delimiter;
    }

    private String createGeneratedSecret() {
        return "CDK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private CredentialCategoryEntity resolveCategory(Long id, String defaultCategoryCode) {
        if (id != null) {
            return loadCategory(id);
        }
        CredentialCategoryEntity category = credentialCategoryMapper.findByCode(defaultCategoryCode);
        if (category == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND, "默认凭证分类不存在");
        }
        return category;
    }

    private CredentialCategoryEntity loadCategory(Long id) {
        if (id == null || id <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
        CredentialCategoryEntity category = credentialCategoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND, "凭证分类不存在");
        }
        return category;
    }

    private CredentialBatchEntity loadBatch(Long id) {
        if (id == null || id <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
        CredentialBatchEntity batch = credentialBatchMapper.findById(id);
        if (batch == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_BATCH_NOT_FOUND);
        }
        return batch;
    }

    private CredentialItemEntity loadItem(Long id) {
        if (id == null || id <= 0L) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
        CredentialItemEntity item = credentialItemMapper.findById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.CREDENTIAL_ITEM_NOT_FOUND);
        }
        return item;
    }

    private String defaultStartDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now().format(DATE_TIME_FORMATTER);
        }
        return validateDateTime(value);
    }

    private String defaultExpireDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_PERMANENT_EXPIRE_AT;
        }
        return validateDateTime(value);
    }

    private String validateDateTime(String value) {
        try {
            LocalDateTime.parse(value, DATE_TIME_FORMATTER);
            return value;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "时间格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String defaultBatchName(String batchName, String prefix) {
        if (StringUtils.hasText(batchName)) {
            return batchName.trim();
        }
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private List<CredentialCategoryVo> toCategoryVos(List<CredentialCategoryEntity> entities) {
        List<CredentialCategoryVo> records = new ArrayList<CredentialCategoryVo>();
        for (CredentialCategoryEntity entity : entities) {
            records.add(toCategoryVo(entity));
        }
        return records;
    }

    private CredentialCategoryVo toCategoryVo(CredentialCategoryEntity entity) {
        CredentialCategoryVo vo = new CredentialCategoryVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setFulfillmentType(entity.getFulfillmentType());
        vo.setGenerationMode(entity.getGenerationMode());
        vo.setPayloadSchema(entity.getPayloadSchema());
        vo.setImportConfig(entity.getImportConfig());
        vo.setExtractPolicy(entity.getExtractPolicy());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<CredentialBatchVo> toBatchVos(List<CredentialBatchEntity> entities) {
        List<CredentialBatchVo> records = new ArrayList<CredentialBatchVo>();
        for (CredentialBatchEntity entity : entities) {
            records.add(toBatchVo(entity));
        }
        return records;
    }

    private CredentialBatchVo toBatchVo(CredentialBatchEntity entity) {
        CredentialBatchVo vo = new CredentialBatchVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setBatchNo(entity.getBatchNo());
        vo.setBatchName(entity.getBatchName());
        vo.setCategoryId(String.valueOf(entity.getCategoryId()));
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setCategoryName(entity.getCategoryName());
        vo.setFulfillmentType(entity.getFulfillmentType());
        vo.setGenerationMode(entity.getGenerationMode());
        vo.setPayloadConfig(entity.getPayloadConfig());
        vo.setTotalCount(entity.getTotalCount());
        vo.setAvailableCount(entity.getAvailableCount());
        vo.setConsumedCount(entity.getConsumedCount());
        vo.setLinkedCount(entity.getLinkedCount());
        vo.setValidFrom(entity.getValidFrom());
        vo.setValidTo(entity.getValidTo());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()));
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private List<CredentialItemVo> toItemVos(List<CredentialItemEntity> entities) {
        List<CredentialItemVo> records = new ArrayList<CredentialItemVo>();
        for (CredentialItemEntity entity : entities) {
            records.add(toItemVo(entity));
        }
        return records;
    }

    private CredentialItemVo toItemVo(CredentialItemEntity entity) {
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
        return vo;
    }

    private List<CredentialImportTaskVo> toImportTaskVos(List<CredentialImportTaskEntity> entities) {
        List<CredentialImportTaskVo> records = new ArrayList<CredentialImportTaskVo>();
        for (CredentialImportTaskEntity entity : entities) {
            CredentialImportTaskVo vo = new CredentialImportTaskVo();
            vo.setId(String.valueOf(entity.getId()));
            vo.setTaskNo(entity.getTaskNo());
            vo.setBatchId(entity.getBatchId() == null ? null : String.valueOf(entity.getBatchId()));
            vo.setCategoryId(String.valueOf(entity.getCategoryId()));
            vo.setCategoryName(entity.getCategoryName());
            vo.setDelimiter(entity.getDelimiter());
            vo.setTotalRows(entity.getTotalRows());
            vo.setValidRows(entity.getValidRows());
            vo.setDuplicateRows(entity.getDuplicateRows());
            vo.setInvalidRows(entity.getInvalidRows());
            vo.setImportHash(entity.getImportHash());
            vo.setResultSummary(entity.getResultSummary());
            vo.setStatus(entity.getStatus());
            vo.setCreatedBy(entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()));
            vo.setCreatedAt(entity.getCreatedAt());
            vo.setUpdatedAt(entity.getUpdatedAt());
            records.add(vo);
        }
        return records;
    }

    private List<CredentialRedeemRecordVo> toRedeemRecordVos(List<CredentialRedeemRecordEntity> entities) {
        List<CredentialRedeemRecordVo> records = new ArrayList<CredentialRedeemRecordVo>();
        for (CredentialRedeemRecordEntity entity : entities) {
            CredentialRedeemRecordVo vo = new CredentialRedeemRecordVo();
            vo.setId(String.valueOf(entity.getId()));
            vo.setRecordNo(entity.getRecordNo());
            vo.setItemId(entity.getItemId() == null ? null : String.valueOf(entity.getItemId()));
            vo.setBatchId(entity.getBatchId() == null ? null : String.valueOf(entity.getBatchId()));
            vo.setCategoryId(entity.getCategoryId() == null ? null : String.valueOf(entity.getCategoryId()));
            vo.setUserId(String.valueOf(entity.getUserId()));
            vo.setPoints(entity.getPoints());
            vo.setIdempotencyKey(entity.getIdempotencyKey());
            vo.setClientIp(entity.getClientIp());
            vo.setUserAgentHash(entity.getUserAgentHash());
            vo.setDeviceFingerprint(entity.getDeviceFingerprint());
            vo.setLedgerNo(entity.getLedgerNo());
            vo.setStatus(entity.getStatus());
            vo.setFailureReason(entity.getFailureReason());
            vo.setCreatedAt(entity.getCreatedAt());
            records.add(vo);
        }
        return records;
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < DEFAULT_PAGE_NO ? DEFAULT_PAGE_NO : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < DEFAULT_PAGE_NO) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int offset(int pageNo, int pageSize) {
        return (pageNo - 1) * pageSize;
    }

    private String createNo(String prefix) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + LocalDateTime.now().format(NO_TIME_FORMATTER) + uuid.substring(0, UUID_FRAGMENT_LENGTH).toUpperCase();
    }

    private String safeJsonText(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class ImportSecret {
        private final Integer lineNo;
        private final String secretText;

        ImportSecret(Integer lineNo, String secretText) {
            this.lineNo = lineNo;
            this.secretText = secretText;
        }
    }

    private class ImportParseResult {
        private int totalRows;
        private int validRows;
        private int duplicateRows;
        private int invalidRows;
        private String importHash;
        private final List<CredentialImportPreviewItemVo> previews = new ArrayList<CredentialImportPreviewItemVo>();
        private final List<String> duplicateMessages = new ArrayList<String>();
        private final List<String> invalidMessages = new ArrayList<String>();
        private final List<ImportSecret> validSecrets = new ArrayList<ImportSecret>();

        private void addPreview(Integer lineNo, String secretMask, String status, String message) {
            if (PREVIEW_STATUS_DUPLICATE.equals(status)) {
                duplicateRows++;
                duplicateMessages.add("第 " + lineNo + " 行：" + message);
            }
            if (PREVIEW_STATUS_INVALID.equals(status)) {
                invalidRows++;
                invalidMessages.add("第 " + lineNo + " 行：" + message);
            }
            if (previews.size() >= PREVIEW_LIMIT) {
                return;
            }
            CredentialImportPreviewItemVo item = new CredentialImportPreviewItemVo();
            item.setLineNo(lineNo);
            item.setSecretMask(secretMask);
            item.setStatus(status);
            item.setMessage(message);
            previews.add(item);
        }

        private CredentialImportPreviewVo toVo() {
            CredentialImportPreviewVo vo = new CredentialImportPreviewVo();
            vo.setTotalRows(totalRows);
            vo.setValidRows(validRows);
            vo.setDuplicateRows(duplicateRows);
            vo.setInvalidRows(invalidRows);
            vo.setImportHash(importHash);
            vo.setPreviews(previews);
            vo.setDuplicateMessages(duplicateMessages);
            vo.setInvalidMessages(invalidMessages);
            return vo;
        }
    }
}
