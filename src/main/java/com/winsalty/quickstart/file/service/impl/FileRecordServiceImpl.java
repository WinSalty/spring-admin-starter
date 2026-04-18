package com.winsalty.quickstart.file.service.impl;

import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
import com.winsalty.quickstart.common.api.PageResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileRecordServiceImpl implements FileRecordService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "txt", "csv", "xls", "xlsx", "doc", "docx", "zip"
    ));

    private final FileRecordMapper fileRecordMapper;
    private final String uploadDir;

    public FileRecordServiceImpl(FileRecordMapper fileRecordMapper,
                                 @Value("${app.file.upload-dir:uploads}") String uploadDir) {
        this.fileRecordMapper = fileRecordMapper;
        this.uploadDir = uploadDir;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(4041, "上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(4042, "文件大小不能超过 10MB");
        }
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = resolveExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(4043, "文件类型不支持");
        }
        File directory = new File(uploadDir);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new BusinessException(5001, "上传目录创建失败");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        File target = new File(directory, storedName);
        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw new BusinessException(5002, "文件保存失败");
        }
        FileRecordEntity entity = new FileRecordEntity();
        entity.setFileCode("F" + System.currentTimeMillis());
        entity.setOriginalName(originalName);
        entity.setStoredName(storedName);
        entity.setFilePath(target.getPath());
        entity.setContentType(file.getContentType());
        entity.setExtension(extension);
        entity.setSizeBytes(file.getSize());
        entity.setStatus("active");
        entity.setCreatedBy(resolveCurrentUsername());
        fileRecordMapper.insert(entity);
        return toVo(load(entity.getId()));
    }

    @Override
    public PageResponse<FileRecordVo> getPage(FileListRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<FileRecordEntity> entities = fileRecordMapper.findPage(request.getKeyword(), request.getStatus(), offset, pageSize);
        long total = fileRecordMapper.countPage(request.getKeyword(), request.getStatus());
        return new PageResponse<FileRecordVo>(toVoList(entities), pageNo, pageSize, total);
    }

    @Override
    public FileRecordVo getDetail(String id) {
        return toVo(load(parseId(id)));
    }

    @Override
    public Resource loadDownloadResource(String id) {
        FileRecordEntity entity = load(parseId(id));
        File file = new File(entity.getFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(4044, "文件不存在");
        }
        return new FileSystemResource(file);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo delete(String id) {
        Long fileId = parseId(id);
        FileRecordVo vo = toVo(load(fileId));
        fileRecordMapper.softDelete(fileId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecordVo updateStatus(String id, FileStatusRequest request) {
        Long fileId = parseId(id);
        load(fileId);
        fileRecordMapper.updateStatus(fileId, request.getStatus());
        return toVo(load(fileId));
    }

    private String resolveExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            throw new BusinessException(4043, "文件类型不支持");
        }
        return originalName.substring(index + 1).toLowerCase();
    }

    private String resolveCurrentUsername() {
        AuthUser authUser = AuthContext.get();
        return authUser == null ? "system" : authUser.getUsername();
    }

    private FileRecordEntity load(Long id) {
        FileRecordEntity entity = fileRecordMapper.findById(id);
        if (entity == null) {
            throw new BusinessException(4044, "文件记录不存在");
        }
        return entity;
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (Exception exception) {
            throw new BusinessException(4001, "id 不合法");
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
