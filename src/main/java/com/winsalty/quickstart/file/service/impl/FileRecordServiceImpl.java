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
import com.winsalty.quickstart.file.vo.PrivateDownloadUrlVo;
import com.winsalty.quickstart.infra.storage.AliyunOssObjectStorageUtil;
import com.winsalty.quickstart.infra.storage.AliyunOssStorageProperties;
import com.winsalty.quickstart.infra.storage.ObjectStorageUploadResult;
import com.winsalty.quickstart.infra.storage.ObjectStorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * 按配置使用本地目录或阿里云 OSS 对象存储保存文件，数据库记录文件元数据和访问地址。
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
    private static final String STORAGE_TYPE_ALIYUN_OSS = "aliyun-oss";
    private static final String BUCKET_TYPE_PUBLIC = "public";
    private static final String BUCKET_TYPE_PRIVATE = "private";
    private static final String ACCESS_POLICY_PUBLIC_READ = "public_read";
    private static final String ACCESS_POLICY_PRIVATE_READ = "private_read";
    private static final String LOCAL_PUBLIC_BUCKET_NAME = "local-public";
    private static final String LOCAL_PRIVATE_BUCKET_NAME = "local-private";
    private static final String DOWNLOAD_MODE_SIGNED_URL = "signed_url";
    private static final String DOWNLOAD_MODE_PROXY_STREAM = "proxy_stream";
    private static final String HASH_ALGORITHM_SHA_256 = "SHA-256";

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
    private final AliyunOssObjectStorageUtil aliyunOssObjectStorageUtil;
    private final AliyunOssStorageProperties aliyunOssStorageProperties;
    private final ObjectStorageProperties objectStorageProperties;
    private final String uploadDir;

    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper,
                                 AliyunOssObjectStorageUtil aliyunOssObjectStorageUtil,
                                 AliyunOssStorageProperties aliyunOssStorageProperties,
                                 ObjectStorageProperties objectStorageProperties,
                                 @Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileRecordMapper = fileRecordMapper;
        this.aliyunOssObjectStorageUtil = aliyunOssObjectStorageUtil;
        this.aliyunOssStorageProperties = aliyunOssStorageProperties;
        this.objectStorageProperties = objectStorageProperties;
        this.uploadDir = uploadDir;
    }

    /**
     * 上传文件主流程：大小校验、扩展名白名单、MIME 校验、本地落盘、写入元数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo upload(MultipartFile file) {
        return uploadInternal(file, ALLOWED_EXTENSIONS, "file", BUCKET_TYPE_PUBLIC);
    }

    /**
     * 上传用户头像，仅允许图片类型。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo uploadAvatar(MultipartFile file) {
        return uploadInternal(file, ALLOWED_AVATAR_EXTENSIONS, "avatar", BUCKET_TYPE_PUBLIC);
    }

    /**
     * 上传私有文件，文件访问必须通过后端鉴权后的下载接口完成。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo uploadPrivate(MultipartFile file) {
        return uploadInternal(file, ALLOWED_EXTENSIONS, "private", BUCKET_TYPE_PRIVATE);
    }

    private FileRecordVo uploadInternal(MultipartFile file, Set<String> allowedExtensions, String bizType, String bucketType) {
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
        byte[] fileBytes = readFileBytes(file);
        validateMagicBytes(fileBytes, extension);
        String contentHash = sha256Hex(fileBytes);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        ObjectStorageUploadResult uploadResult = storeFile(file, fileBytes, storedName, extension, contentHash, bizType, bucketType);
        FileRecordEntity entity = new FileRecordEntity();
        entity.setFileCode("F" + System.currentTimeMillis());
        entity.setOriginalName(originalName);
        // storedName 使用 UUID，避免用户上传同名文件互相覆盖。
        entity.setStoredName(storedName);
        entity.setFilePath(uploadResult.getFilePath());
        entity.setStorageType(uploadResult.getStorageType());
        entity.setBucketType(uploadResult.getBucketType());
        entity.setBucketName(uploadResult.getBucketName());
        entity.setAccessPolicy(uploadResult.getAccessPolicy());
        entity.setObjectKey(uploadResult.getObjectKey());
        entity.setFileUrl(uploadResult.getFileUrl());
        entity.setContentHash(contentHash);
        entity.setContentType(file.getContentType());
        entity.setExtension(extension);
        entity.setSizeBytes(file.getSize());
        entity.setStatus(CommonStatusConstants.ACTIVE);
        entity.setCreatedBy(resolveCurrentUsername());
        fileRecordMapper.insert(entity);
        log.info("file upload success, bizType={}, storageType={}, objectKey={}, contentHash={}, originalName={}, sizeBytes={}",
                bizType, entity.getStorageType(), entity.getObjectKey(), entity.getContentHash(), entity.getOriginalName(), entity.getSizeBytes());
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
        if (STORAGE_TYPE_ALIYUN_OSS.equals(entity.getStorageType())) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "阿里云 OSS 文件请使用文件访问地址或私有下载地址");
        }
        File file = resolveLocalFile(entity.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return new FileSystemResource(file);
    }

    /**
     * 加载本地公共文件资源，仅允许读取 public 逻辑空间内的有效文件记录。
     */
    @Override
    public Resource loadPublicResource(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        FileRecordEntity entity = fileRecordMapper.findPublicLocalByObjectKey(normalizedObjectKey);
        if (entity == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return loadDownloadResource(String.valueOf(entity.getId()));
    }

    /**
     * 创建私有文件下载地址，OSS 返回临时签名 URL，本地存储返回后端代理下载地址。
     */
    @Override
    public PrivateDownloadUrlVo createPrivateDownloadUrl(String id) {
        FileRecordEntity entity = load(parseId(id));
        if (!BUCKET_TYPE_PRIVATE.equals(entity.getBucketType())) {
            throw new BusinessException(ErrorCode.REQUEST_PARAM_INVALID, "非私有文件不需要生成临时下载地址");
        }
        PrivateDownloadUrlVo vo = new PrivateDownloadUrlVo();
        vo.setFileId(String.valueOf(entity.getId()));
        if (STORAGE_TYPE_ALIYUN_OSS.equals(entity.getStorageType())) {
            long expireSeconds = aliyunOssStorageProperties.getPrivateUrlExpireSeconds();
            vo.setDownloadUrl(aliyunOssObjectStorageUtil.generatePresignedUrl(entity.getBucketName(), entity.getObjectKey(), expireSeconds));
            vo.setExpireSeconds(expireSeconds);
            vo.setDownloadMode(DOWNLOAD_MODE_SIGNED_URL);
            return vo;
        }
        vo.setDownloadUrl("/api/file/private/" + entity.getId() + "/download");
        vo.setExpireSeconds(objectStorageProperties.getLocal().getPrivateUrlExpireSeconds());
        vo.setDownloadMode(DOWNLOAD_MODE_PROXY_STREAM);
        return vo;
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
     * 根据对象存储开关选择本地或阿里云 OSS 存储；相同内容 Hash 直接复用已有对象，减少重复上传。
     */
    private ObjectStorageUploadResult storeFile(MultipartFile file,
                                                byte[] fileBytes,
                                                String storedName,
                                                String extension,
                                                String contentHash,
                                                String bizType,
                                                String bucketType) {
        String storageType = objectStorageProperties.isEnabled() ? STORAGE_TYPE_ALIYUN_OSS : STORAGE_TYPE_LOCAL;
        FileRecordEntity reusable = fileRecordMapper.findReusableByContentHash(contentHash, storageType, bucketType);
        if (reusable != null) {
            log.info("file content hash reused, storageType={}, bucketType={}, objectKey={}, contentHash={}",
                    reusable.getStorageType(), reusable.getBucketType(), reusable.getObjectKey(), contentHash);
            return toUploadResult(reusable);
        }
        if (objectStorageProperties.isEnabled()) {
            return uploadToAliyunOss(file, fileBytes, contentHash, extension, bizType, bucketType);
        }
        return saveToLocal(fileBytes, storedName, contentHash, extension, bizType, bucketType);
    }

    private ObjectStorageUploadResult uploadToAliyunOss(MultipartFile file,
                                                        byte[] fileBytes,
                                                        String contentHash,
                                                        String extension,
                                                        String bizType,
                                                        String bucketType) {
        String objectKey = buildAliyunOssObjectKey(contentHash, extension, bizType, bucketType);
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            log.info("aliyun oss upload start, bucketType={}, objectKey={}, sizeBytes={}", bucketType, objectKey, file.getSize());
            if (BUCKET_TYPE_PRIVATE.equals(bucketType)) {
                return aliyunOssObjectStorageUtil.uploadPrivate(inputStream, objectKey, file.getContentType(), file.getSize());
            }
            return aliyunOssObjectStorageUtil.uploadPublic(inputStream, objectKey, file.getContentType(), file.getSize());
        } catch (IOException exception) {
            log.error("aliyun oss upload input stream open failed, objectKey={}", objectKey);
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private ObjectStorageUploadResult saveToLocal(byte[] fileBytes,
                                                  String storedName,
                                                  String contentHash,
                                                  String extension,
                                                  String bizType,
                                                  String bucketType) {
        String objectKey = buildLocalObjectKey(contentHash, extension, bizType, bucketType);
        File rootDirectory = resolveLocalRootDirectory();
        File target = new File(rootDirectory, objectKey);
        File directory = target.getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            throw new BusinessException(ErrorCode.DIRECTORY_CREATE_FAILED);
        }
        try {
            // 先落盘再写数据库；若落盘失败，事务不会留下文件元数据。
            Files.write(target.toPath(), fileBytes);
        } catch (IOException exception) {
            log.error("local file save failed, path={}", target.getPath());
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
        ObjectStorageUploadResult result = new ObjectStorageUploadResult();
        result.setStorageType(STORAGE_TYPE_LOCAL);
        result.setBucketType(bucketType);
        result.setBucketName(BUCKET_TYPE_PRIVATE.equals(bucketType) ? LOCAL_PRIVATE_BUCKET_NAME : LOCAL_PUBLIC_BUCKET_NAME);
        result.setAccessPolicy(BUCKET_TYPE_PRIVATE.equals(bucketType) ? ACCESS_POLICY_PRIVATE_READ : ACCESS_POLICY_PUBLIC_READ);
        result.setObjectKey(objectKey);
        result.setFilePath(target.getPath());
        result.setFileUrl(BUCKET_TYPE_PRIVATE.equals(bucketType) ? "" : buildLocalPublicUrl(objectKey));
        return result;
    }

    private ObjectStorageUploadResult toUploadResult(FileRecordEntity entity) {
        ObjectStorageUploadResult result = new ObjectStorageUploadResult();
        result.setStorageType(entity.getStorageType());
        result.setBucketType(entity.getBucketType());
        result.setBucketName(entity.getBucketName());
        result.setAccessPolicy(entity.getAccessPolicy());
        result.setObjectKey(entity.getObjectKey());
        result.setFilePath(entity.getFilePath());
        result.setFileUrl(entity.getFileUrl());
        return result;
    }

    private String buildAliyunOssObjectKey(String contentHash, String extension, String bizType, String bucketType) {
        String prefix = aliyunOssStorageProperties.getKeyPrefix();
        String hashPath = bucketType + "/" + bizType + "/" + contentHash.substring(0, 2) + "/" + contentHash + "." + extension;
        if (!StringUtils.hasText(prefix)) {
            return hashPath;
        }
        String normalizedPrefix = prefix.trim();
        while (normalizedPrefix.startsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(1);
        }
        while (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix + "/" + hashPath;
    }

    private String buildLocalObjectKey(String contentHash, String extension, String bizType, String bucketType) {
        return bucketType + "/" + bizType + "/" + contentHash.substring(0, 2) + "/" + contentHash + "." + extension;
    }

    private String buildLocalPublicUrl(String objectKey) {
        String baseUrl = objectStorageProperties.getLocal().getPublicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "/api/file/public";
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + objectKey;
    }

    private File resolveLocalRootDirectory() {
        String rootPath = objectStorageProperties.getLocal().getRootPath();
        if (!StringUtils.hasText(rootPath)) {
            rootPath = uploadDir;
        }
        return new File(rootPath);
    }

    private File resolveLocalFile(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        File rootDirectory = resolveLocalRootDirectory();
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(filePath);
        }
        try {
            File canonicalRoot = rootDirectory.getCanonicalFile();
            File canonicalFile = file.getCanonicalFile();
            String rootPath = canonicalRoot.getPath();
            String targetPath = canonicalFile.getPath();
            if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            return canonicalFile;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        String normalized = objectKey.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("../") || normalized.contains("..\\")) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!normalized.startsWith(BUCKET_TYPE_PUBLIC + "/")) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return normalized;
    }

    /**
     * 对二进制文件校验魔数，对文本文件校验是否包含明显二进制控制字节。
     */
    private void validateMagicBytes(byte[] fileBytes, String extension) {
        byte[] signature = readSignature(fileBytes);
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
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private byte[] readSignature(byte[] fileBytes) {
        if (fileBytes.length == 0) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        return Arrays.copyOf(fileBytes, Math.min(fileBytes.length, FILE_SIGNATURE_READ_LENGTH));
    }

    private String sha256Hex(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM_SHA_256);
            return toHex(digest.digest(fileBytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte current : bytes) {
            String hex = Integer.toHexString(current & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
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
        vo.setBucketType(entity.getBucketType());
        vo.setBucketName(entity.getBucketName());
        vo.setAccessPolicy(entity.getAccessPolicy());
        vo.setObjectKey(entity.getObjectKey());
        vo.setFileUrl(entity.getFileUrl());
        vo.setContentHash(entity.getContentHash());
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
