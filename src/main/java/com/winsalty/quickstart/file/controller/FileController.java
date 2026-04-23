package com.winsalty.quickstart.file.controller;

import com.winsalty.quickstart.auth.service.support.AuthRateLimitService;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.base.BaseController;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.util.IpUtils;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.file.dto.FileListRequest;
import com.winsalty.quickstart.file.dto.FileStatusRequest;
import com.winsalty.quickstart.file.service.FileRecordService;
import com.winsalty.quickstart.file.vo.FileRecordVo;
import com.winsalty.quickstart.file.vo.ObjectStorageStatusVo;
import com.winsalty.quickstart.infra.storage.ObjectStorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 文件管理控制器。
 * 提供本地文件上传、列表、下载、软删除和状态切换接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/file")
public class FileController extends BaseController {

    private final FileRecordService fileRecordService;
    private final AuthRateLimitService authRateLimitService;
    private final ObjectStorageProperties objectStorageProperties;

    public FileController(FileRecordService fileRecordService,
                          AuthRateLimitService authRateLimitService,
                          ObjectStorageProperties objectStorageProperties) {
        this.fileRecordService = fileRecordService;
        this.authRateLimitService = authRateLimitService;
        this.objectStorageProperties = objectStorageProperties;
    }

    /**
     * 文件上传仅管理员可用，文件类型和大小由服务层校验。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileRecordVo> upload(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        authRateLimitService.checkFileUpload(currentUsername(), IpUtils.getClientIp(request));
        return ApiResponse.success("上传成功", fileRecordService.upload(file));
    }

    /**
     * 当前登录用户头像上传，普通用户可用，服务层仅允许图片类型。
     */
    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileRecordVo> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                  HttpServletRequest request) {
        authRateLimitService.checkFileUpload(currentUsername(), IpUtils.getClientIp(request));
        return ApiResponse.success("头像上传成功", fileRecordService.uploadAvatar(file));
    }

    /**
     * 查询对象存储开关状态，前端据此决定是否展示头像上传入口。
     */
    @GetMapping("/object-storage/status")
    public ApiResponse<ObjectStorageStatusVo> objectStorageStatus() {
        ObjectStorageStatusVo vo = new ObjectStorageStatusVo();
        vo.setEnabled(objectStorageProperties.isEnabled());
        vo.setProvider(objectStorageProperties.getProvider());
        return ApiResponse.success("获取成功", vo);
    }

    /**
     * 文件记录分页列表。
     */
    @GetMapping("/list")
    public ApiResponse<PageResponse<FileRecordVo>> list(@Validated FileListRequest request) {
        return ApiResponse.success("获取成功", fileRecordService.getPage(request));
    }

    /**
     * 文件下载会按原始文件名设置 attachment 响应头。
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable("id") String id) {
        FileRecordVo detail = fileRecordService.getDetail(id);
        if ("aliyun-oss".equals(detail.getStorageType()) && detail.getFileUrl() != null && !detail.getFileUrl().isEmpty()) {
            return ResponseEntity.status(302).location(URI.create(detail.getFileUrl())).build();
        }
        Resource resource = fileRecordService.loadDownloadResource(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(detail.getOriginalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * 头像图片访问入口。仅用于浏览器展示已保存头像，阿里云 OSS 文件直接跳转外链。
     */
    @GetMapping("/avatar/{id}")
    public ResponseEntity<?> avatar(@PathVariable("id") String id) {
        FileRecordVo detail = fileRecordService.getDetail(id);
        if (detail.getContentType() == null || !detail.getContentType().startsWith("image/")) {
            throw new BusinessException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        if ("aliyun-oss".equals(detail.getStorageType()) && detail.getFileUrl() != null && !detail.getFileUrl().isEmpty()) {
            return ResponseEntity.status(302).location(URI.create(detail.getFileUrl())).build();
        }
        Resource resource = fileRecordService.loadDownloadResource(id);
        MediaType mediaType = detail.getContentType() == null || detail.getContentType().isEmpty()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(detail.getContentType());
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    /**
     * 软删除文件记录，不直接删除磁盘文件。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public ApiResponse<FileRecordVo> delete(@PathVariable("id") String id) {
        return ApiResponse.success("删除成功", fileRecordService.delete(id));
    }

    /**
     * 切换文件记录状态，例如启用或禁用下载入口。
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/status")
    public ApiResponse<FileRecordVo> status(@PathVariable("id") String id, @Valid @RequestBody FileStatusRequest request) {
        return ApiResponse.success("状态已更新", fileRecordService.updateStatus(id, request));
    }
}
