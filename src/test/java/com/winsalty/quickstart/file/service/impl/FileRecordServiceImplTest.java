package com.winsalty.quickstart.file.service.impl;

import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.file.mapper.FileRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 文件上传服务测试。
 * 覆盖扩展名、MIME 与文件头签名的联合校验，防止伪造文件类型绕过上传限制。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
class FileRecordServiceImplTest {

    @Test
    void uploadShouldRejectFileWhenMagicBytesDoNotMatchExtension() {
        FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
        FileRecordServiceImpl service = new FileRecordServiceImpl(fileRecordMapper, "target/test-uploads");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "%PDF-1.4 fake".getBytes()
        );

        assertThrows(BusinessException.class, () -> service.upload(file));

        verifyNoInteractions(fileRecordMapper);
    }
}
