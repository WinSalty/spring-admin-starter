package com.winsalty.quickstart.file.service.impl;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.common.base.BaseService;
import com.winsalty.quickstart.common.constant.CommonStatusConstants;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.file.dto.FileListRequest;
import com.winsalty.quickstart.file.dto.FileStatusRequest;
import com.winsalty.quickstart.file.entity.FileRecordEntity;
import com.winsalty.quickstart.file.mapper.FileRecordMapper;
import com.winsalty.quickstart.file.service.FileRecordService;
import com.winsalty.quickstart.file.vo.FileRecordVo;
import com.winsalty.quickstart.infra.storage.ObjectStorageUploadResult;
import com.winsalty.quickstart.infra.storage.QiniuObjectStorageUtil;
import com.winsalty.quickstart.infra.storage.QiniuStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文件记录服务实现。
 * 按配置使用本地目录或七牛云对象存储保存文件，数据库记录文件元数据和访问地址。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Service
public class FileRecordServiceImpl extends BaseService implements FileRecordService {

    private static final Logger log = LoggerFactory.getLogger(FileRecordServiceImpl.class);
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int FILE_SIGNATURE_READ_LENGTH = 16;
    private static final String STORAGE_TYPE_LOCAL = "local";
    private static final String STORAGE_TYPE_QINIU = "qiniu";

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "csv", "xls", "xlsx", "doc", "docx", "zip"
    ));
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = new HashSet<String>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    ));

    // 扩展名与允许的 MIME type 映射，用于双重校验防止伪造扩展名绕过
    private static final Map<String, String> EXTENSION_MIME_MAP = new HashMap<String, String>();
    static {
        EXTENSION_MIME_MAP.put("jpg", "image/jpeg");
        EXTENSION_MIME_MAP.put("jpeg", "image/jpeg");
        EXTENSION_MIME_MAP.put("png", "image/png");
        EXTENSION_MIME_MAP.put("gif", "image/gif");
        EXTENSION_MIME_MAP.put("webp", "image/webp");
        EXTENSION_MIME_MAP.put("pdf", "application/pdf");
        EXTENSION_MIME_MAP.put("txt", "text/plain");
        EXTENSION_MIME_MAP.put("csv", "text/csv");
        EXTENSION_MIME_MAP.put("xls", "application/vnd.ms-excel");
        EXTENSION_MIME_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        EXTENSION_MIME_MAP.put("doc", "application/msword");
        EXTENSION_MIME_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_MIME_MAP.put("zip", "application/zip");
    }

    private static final Map<String, List<byte[]>> EXTENSION_MAGIC_BYTES_MAP = new HashMap<String, List<byte[]>>();
    static {
        EXTENSION_MAGIC_BYTES_MAP.put("jpg", Arrays.asList(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("jpeg", EXTENSION_MAGIC_BYTES_MAP.get("jpg"));
        EXTENSION_MAGIC_BYTES_MAP.put("png", Arrays.asList(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("gif", Arrays.asList(
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61},
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("webp", Arrays.asList(
                new byte[]{0x52, 0x49, 0x46, 0x46}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("pdf", Arrays.asList(
                new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("zip", Arrays.asList(
                new byte[]{0x50, 0x4B, 0x03, 0x04},
                new byte[]{0x50, 0x4B, 0x05, 0x06},
                new byte[]{0x50, 0x4B, 0x07, 0x08}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("xlsx", EXTENSION_MAGIC_BYTES_MAP.get("zip"));
        EXTENSION_MAGIC_BYTES_MAP.put("docx", EXTENSION_MAGIC_BYTES_MAP.get("zip"));
        EXTENSION_MAGIC_BYTES_MAP.put("xls", Arrays.asList(
                new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1}
        ));
        EXTENSION_MAGIC_BYTES_MAP.put("doc", EXTENSION_MAGIC_BYTES_MAP.get("xls"));
    }

    private final FileRecordMapper fileRecordMapper;
    private final QiniuObjectStorageUtil qiniuObjectStorageUtil;
    private final QiniuStorageProperties qiniuStorageProperties;
    private final String uploadDir;
    private final String storageType;

    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper,
                                 QiniuObjectStorageUtil qiniuObjectStorageUtil,
                                 QiniuStorageProperties qiniuStorageProperties,
                                 @Value("${app.file.upload-dir:uploads}") String uploadDir,
                                 @Value("${app.file.storage-type:local}") String storageType) {
        this.fileRecordMapper = fileRecordMapper;
        this.qiniuObjectStorageUtil = qiniuObjectStorageUtil;
        this.qiniuStorageProperties = qiniuStorageProperties;
        this.uploadDir = uploadDir;
        this.storageType = normalizeStorageType(storageType);
    }

    /**
     * 上传文件主流程：大小校验、扩展名白名单、MIME 校验、本地落盘、写入元数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo upload(MultipartFile file) {
        return uploadInternal(file, ALLOWED_EXTENSIONS, "file");
    }

    /**
     * 上传用户头像，仅允许图片类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo uploadAvatar(MultipartFile file) {
        return uploadInternal(file, ALLOWED_AVATAR_EXTENSIONS, "avatar");
    }

    private FileRecordVo uploadInternal(MultipartFile file, Set<String> allowedExtensions, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = resolveExtension(originalName);
        if (!allowedExtensions.contains(extension)) {
            // 扩展名先过白名单，快速拒绝脚本、可执行文件等明显不允许的类型。
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        // 校验 Content-Type 与扩展名是否匹配，防止伪造扩展名绕过校验
        String expectedMime = EXTENSION_MIME_MAP.get(extension);
        String actualMime = file.getContentType();
        if (expectedMime != null && !expectedMime.equals(actualMime)) {
            // Content-Type 不能完全防伪，但能挡住最常见的“改后缀上传”误用。
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        validateMagicBytes(file, extension);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        ObjectStorageUploadResult uploadResult = storeFile(file, storedName, extension);
        FileRecordEntity entity = new FileRecordEntity();
        entity.setFileCode("F" + System.currentTimeMillis());
        entity.setOriginalName(originalName);
        // storedName 使用 UUID，避免用户上传同名文件互相覆盖。
        entity.setStoredName(storedName);
        entity.setFilePath(uploadResult.getFilePath());
        entity.setStorageType(uploadResult.getStorageType());
        entity.setObjectKey(uploadResult.getObjectKey());
        entity.setFileUrl(uploadResult.getFileUrl());
        entity.setContentType(file.getContentType());
        entity.setExtension(extension);
        entity.setSizeBytes(file.getSize());
        entity.setStatus(CommonStatusConstants.ACTIVE);
        entity.setCreatedBy(resolveCurrentUsername());
        fileRecordMapper.insert(entity);
        log.info("file upload success, bizType={}, storageType={}, objectKey={}, originalName={}, sizeBytes={}",
                bizType, entity.getStorageType(), entity.getObjectKey(), entity.getOriginalName(), entity.getSizeBytes());
        return toVo(load(entity.getId()));
    }

    /**
     * 文件分页列表，限制最大 pageSize，避免一次请求拉取过多记录。
     */
    @Override
    public PageResponse<FileRecordVo> getPage(FileListRequest request) {
        int pageNo = (request.getPageNo() == null || request.getPageNo() < 1) ? DEFAULT_PAGE_NO : request.getPageNo();
        int pageSize = (request.getPageSize() == null || request.getPageSize() < 1) ? DEFAULT_PAGE_SIZE
                : Math.min(request.getPageSize(), MAX_PAGE_SIZE);
        int offset = (pageNo - 1) * pageSize;
        List<FileRecordEntity> entities = fileRecordMapper.findPage(request.getKeyword(), request.getStatus(), offset, pageSize);
        long total = fileRecordMapper.countPage(request.getKeyword(), request.getStatus());
        return new PageResponse<FileRecordVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public FileRecordVo getDetail(String id) {
        return toVo(load(parseId(id)));
    }

    /**
     * 加载可下载资源。若数据库记录存在但磁盘文件缺失，返回文件不存在业务错误。
     */
    @Override
    public Resource loadDownloadResource(String id) {
        FileRecordEntity entity = load(parseId(id));
        if (STORAGE_TYPE_QINIU.equals(entity.getStorageType())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "七牛云文件请使用 fileUrl 访问");
        }
        File file = new File(entity.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return new FileSystemResource(file);
    }

    /**
     * 软删除文件记录，返回删除前的元数据供前端回显。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo delete(String id) {
        Long fileId = parseId(id);
        FileRecordVo vo = toVo(load(fileId));
        // 只软删除数据库记录，不立即删除磁盘文件，便于后续做审计或恢复策略。
        fileRecordMapper.softDelete(fileId);
        return vo;
    }

    /**
     * 更新文件状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo updateStatus(String id, FileStatusRequest request) {
        Long fileId = parseId(id);
        load(fileId);
        fileRecordMapper.updateStatus(fileId, request.getStatus());
        return toVo(load(fileId));
    }

    /**
     * 提取并小写化扩展名，无扩展名或空扩展名视为不支持类型。
     */
    private String resolveExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        return originalName.substring(index + 1).toLowerCase();
    }

    private String resolveCurrentUsername() {
        return currentUsername();
    }

    /**
     * 根据配置选择本地或七牛云存储。
     */
    private ObjectStorageUploadResult storeFile(MultipartFile file, String storedName, String extension) {
        if (STORAGE_TYPE_QINIU.equals(storageType)) {
            return uploadToQiniu(file, storedName);
        }
        return saveToLocal(file, storedName);
    }

    private ObjectStorageUploadResult uploadToQiniu(MultipartFile file, String storedName) {
        String objectKey = buildQiniuObjectKey(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            log.info("qiniu upload start, objectKey={}, sizeBytes={}", objectKey, file.getSize());
            return qiniuObjectStorageUtil.upload(inputStream, objectKey, file.getContentType());
        } catch (IOException exception) {
            log.error("qiniu upload input stream open failed, objectKey={}", objectKey);
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private ObjectStorageUploadResult saveToLocal(MultipartFile file, String storedName) {
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new BusinessException(ErrorCode.DIRECTORY_CREATE_FAILED);
        }
        File target = new File(directory, storedName);
        try {
            // 先落盘再写数据库；若落盘失败，事务不会留下文件元数据。
            file.transferTo(target);
        } catch (IOException exception) {
            log.error("local file save failed, path={}", target.getPath());
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
        ObjectStorageUploadResult result = new ObjectStorageUploadResult();
        result.setStorageType(STORAGE_TYPE_LOCAL);
        result.setObjectKey(storedName);
        result.setFilePath(target.getPath());
        result.setFileUrl("");
        return result;
    }

    private String buildQiniuObjectKey(String storedName) {
        String prefix = qiniuStorageProperties.getKeyPrefix();
        if (!StringUtils.hasText(prefix)) {
            return storedName;
        }
        String normalizedPrefix = prefix.trim();
        while (normalizedPrefix.startsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(1);
        }
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix + "/" + storedName;
    }

    private String normalizeStorageType(String configuredStorageType) {
        if (!StringUtils.hasText(configuredStorageType)) {
            return STORAGE_TYPE_LOCAL;
        }
        String value = configuredStorageType.trim().toLowerCase();
        if (STORAGE_TYPE_LOCAL.equals(value) || STORAGE_TYPE_QINIU.equals(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID, "文件存储类型配置不合法");
    }

    /**
     * 对二进制文件校验魔数，对文本文件校验是否包含明显二进制控制字节。
     */
    private void validateMagicBytes(MultipartFile file, String extension) {
        try {
            byte[] signature = readSignature(file);
            if ("txt".equals(extension) || "csv".equals(extension)) {
                if (!isPlainText(signature)) {
                    throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
                }
                return;
            }
            List<byte[]> allowedMagicBytes = EXTENSION_MAGIC_BYTES_MAP.get(extension);
            if (allowedMagicBytes == null || allowedMagicBytes.isEmpty()) {
                return;
            }
            for (byte[] expected : allowedMagicBytes) {
                if (matchesMagicBytes(signature, expected, extension)) {
                    return;
                }
            }
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private byte[] readSignature(MultipartFile file) throws IOException {
        byte[] signature = new byte[FILE_SIGNATURE_READ_LENGTH];
        try (InputStream inputStream = file.getInputStream()) {
            int readLength = inputStream.read(signature);
            if (readLength <= 0) {
                throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
            }
            return Arrays.copyOf(signature, readLength);
        }
    }

    private boolean matchesMagicBytes(byte[] signature, byte[] expected, String extension) {
        if ("webp".equals(extension)) {
            return signature.length >= 12
                    && hasPrefix(signature, expected)
                    && signature[8] == 0x57
                    && signature[9] == 0x45
                    && signature[10] == 0x42
                    && signature[11] == 0x50;
        }
        return hasPrefix(signature, expected);
    }

    private boolean hasPrefix(byte[] source, byte[] prefix) {
        if (source.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if (source[index] != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isPlainText(byte[] content) {
        for (byte current : content) {
            int value = current & 0xFF;
            if (value == 0) {
                return false;
            }
            if (value < 0x09) {
                return false;
            }
            if (value > 0x0D && value < 0x20) {
                return false;
            }
        }
        return true;
    }

    private FileRecordEntity load(Long id) {
        FileRecordEntity entity = fileRecordMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.FILE_RECORD_NOT_FOUND);
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_ID);
        }
    }

    private List<FileRecordVo> toVoList(List<FileRecordEntity> entities) {
        List<FileRecordVo> records = new ArrayList<FileRecordVo>();
        for (FileRecordEntity entity : entities) {
            records.add(toVo(entity));
        }
        return records;
    }

    private FileRecordVo toVo(FileRecordEntity entity) {
        FileRecordVo vo = new FileRecordVo();
        vo.setId(String.valueOf(entity.getId()));
        vo.setOriginalName(entity.getOriginalName());
        vo.setStoredName(entity.getStoredName());
        vo.setStorageType(entity.getStorageType());
        vo.setObjectKey(entity.getObjectKey());
        vo.setFileUrl(entity.getFileUrl());
        vo.setContentType(entity.getContentType());
        vo.setExtension(entity.getExtension());
        vo.setSizeBytes(entity.getSizeBytes());
        vo.setStatus(entity.getStatus());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
