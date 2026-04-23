package com.winsalty.quickstart.file.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.file.dto.FileListRequest;
import com.winsalty.quickstart.file.dto.FileStatusRequest;
import com.winsalty.quickstart.file.vo.FileRecordVo;
import com.winsalty.quickstart.file.vo.PrivateDownloadUrlVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件记录服务接口。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public interface FileRecordService {
    FileRecordVo upload(MultipartFile file);

    FileRecordVo uploadAvatar(MultipartFile file);

    FileRecordVo uploadPrivate(MultipartFile file);

    PageResponse<FileRecordVo> getPage(FileListRequest request);

    FileRecordVo getDetail(String id);

    Resource loadDownloadResource(String id);

    Resource loadPublicResource(String objectKey);

    PrivateDownloadUrlVo createPrivateDownloadUrl(String id);

    PrivateDownloadUrlVo createProtectedDownloadUrl(String id);

    FileRecordVo delete(String id);

    FileRecordVo updateStatus(String id, FileStatusRequest request);
}
