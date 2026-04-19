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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
 * 使用本地目录保存文件，数据库只记录文件元数据和软删除状态。
 */
@Service
public class FileRecordServiceImpl extends BaseService implements FileRecordService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "csv", "xls", "xlsx", "doc", "docx", "zip"
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

    private final FileRecordMapper fileRecordMapper;
    private final String uploadDir;

    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper,
                                 @Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileRecordMapper = fileRecordMapper;
        this.uploadDir = uploadDir;
    }

    /**
     * 上传文件主流程：大小校验、扩展名白名单、MIME 校验、本地落盘、写入元数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = resolveExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
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
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new BusinessException(ErrorCode.DIRECTORY_CREATE_FAILED);
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        File target = new File(directory, storedName);
        try {
            // 先落盘再写数据库；若落盘失败，事务不会留下文件元数据。
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_SAVE_FAILED);
        }
        FileRecordEntity entity = new FileRecordEntity();
        entity.setFileCode("F" + System.currentTimeMillis());
        entity.setOriginalName(originalName);
        // storedName 使用 UUID，避免用户上传同名文件互相覆盖。
        entity.setStoredName(storedName);
        entity.setFilePath(target.getPath());
        entity.setContentType(file.getContentType());
        entity.setExtension(extension);
        entity.setSizeBytes(file.getSize());
        entity.setStatus(CommonStatusConstants.ACTIVE);
        entity.setCreatedBy(resolveCurrentUsername());
        fileRecordMapper.insert(entity);
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
