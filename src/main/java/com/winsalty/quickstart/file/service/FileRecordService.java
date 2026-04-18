package com.winsalty.quickstart.file.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.file.dto.FileListRequest;
import com.winsalty.quickstart.file.dto.FileStatusRequest;
import com.winsalty.quickstart.file.vo.FileRecordVo;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileRecordService {
    FileRecordVo upload(MultipartFile file);

    PageResponse<FileRecordVo> getPage(FileListRequest request);

    FileRecordVo getDetail(String id);

    Resource loadDownloadResource(String id);

    FileRecordVo delete(String id);

    FileRecordVo updateStatus(String id, FileStatusRequest request);
}
