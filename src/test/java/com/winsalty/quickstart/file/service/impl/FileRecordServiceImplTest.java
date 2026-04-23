package com.winsalty.quickstart.file.service.impl;

import com.winsalty.quickstart.auth.mapper.UserMapper;
import com.winsalty.quickstart.common.exception.BusinessException;
import com.winsalty.quickstart.common.constant.CommonStatusConstants;
import com.winsalty.quickstart.file.entity.FileRecordEntity;
import com.winsalty.quickstart.file.mapper.FileRecordMapper;
import com.winsalty.quickstart.infra.storage.AliyunOssObjectStorageUtil;
import com.winsalty.quickstart.infra.storage.AliyunOssStorageProperties;
import com.winsalty.quickstart.infra.storage.ObjectStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        UserMapper userMapper = mock(UserMapper.class);
        AliyunOssStorageProperties aliyunOssStorageProperties = new AliyunOssStorageProperties();
        FileRecordServiceImpl service = new FileRecordServiceImpl(
                fileRecordMapper,
                userMapper,
                mock(AliyunOssObjectStorageUtil.class),
                aliyunOssStorageProperties,
                new ObjectStorageProperties(),
                "target/test-uploads"
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "%PDF-1.4 fake".getBytes()
        );

        assertThrows(BusinessException.class, () -> service.upload(file));

        verifyNoInteractions(fileRecordMapper, userMapper);
    }

    @Test
    void getPublicAvatarDetailShouldRejectPrivateImage() {
        FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FileRecordServiceImpl service = new FileRecordServiceImpl(
                fileRecordMapper,
                userMapper,
                mock(AliyunOssObjectStorageUtil.class),
                new AliyunOssStorageProperties(),
                new ObjectStorageProperties(),
                "target/test-uploads"
        );
        when(fileRecordMapper.findById(1L)).thenReturn(buildImageRecord("private", CommonStatusConstants.ACTIVE, "/api/file/avatar/1"));

        assertThrows(BusinessException.class, () -> service.getPublicAvatarDetail("1"));
    }

    @Test
    void getPublicAvatarDetailShouldRejectUnreferencedPublicImage() {
        FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FileRecordServiceImpl service = new FileRecordServiceImpl(
                fileRecordMapper,
                userMapper,
                mock(AliyunOssObjectStorageUtil.class),
                new AliyunOssStorageProperties(),
                new ObjectStorageProperties(),
                "target/test-uploads"
        );
        when(fileRecordMapper.findById(2L)).thenReturn(buildImageRecord("public", CommonStatusConstants.ACTIVE, "/api/file/public/public/avatar/aa/hash.png"));
        when(userMapper.countByAvatarUrls(org.mockito.ArgumentMatchers.anyList())).thenReturn(0L);

        assertThrows(BusinessException.class, () -> service.getPublicAvatarDetail("2"));
    }

    @Test
    void getPublicAvatarDetailShouldReturnSanitizedVoForReferencedAvatar() {
        FileRecordMapper fileRecordMapper = mock(FileRecordMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        FileRecordServiceImpl service = new FileRecordServiceImpl(
                fileRecordMapper,
                userMapper,
                mock(AliyunOssObjectStorageUtil.class),
                new AliyunOssStorageProperties(),
                new ObjectStorageProperties(),
                "target/test-uploads"
        );
        FileRecordEntity entity = buildImageRecord("public", CommonStatusConstants.ACTIVE, "/api/file/public/public/avatar/aa/hash.png");
        entity.setId(3L);
        entity.setOriginalName("avatar.png");
        when(fileRecordMapper.findById(3L)).thenReturn(entity);
        when(userMapper.countByAvatarUrls(org.mockito.ArgumentMatchers.anyList())).thenReturn(1L);

        assertEquals("3", service.getPublicAvatarDetail("3").getId());
        assertEquals("avatar.png", service.getPublicAvatarDetail("3").getOriginalName());
    }

    private FileRecordEntity buildImageRecord(String bucketType, String status, String fileUrl) {
        FileRecordEntity entity = new FileRecordEntity();
        entity.setId(1L);
        entity.setBucketType(bucketType);
        entity.setStatus(status);
        entity.setContentType("image/png");
        entity.setFileUrl(fileUrl);
        entity.setStorageType("local");
        entity.setFilePath("target/test-uploads/public/avatar/aa/hash.png");
        entity.setOriginalName("avatar.png");
        entity.setExtension("png");
        return entity;
    }
}
