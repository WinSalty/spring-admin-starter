package com.winsalty.quickstart.file.service.impl;

import com.winsalty.quickstart.auth.mapper.UserMapper;
import com.winsalty.quickstart.auth.security.AuthContext;
import com.winsalty.quickstart.auth.security.AuthUser;
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

    @Test
    void getAuthorizedDetailShouldAllowOwnerUserToReadPrivateBizFile() {
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
        FileRecordEntity entity = buildImageRecord("private", CommonStatusConstants.ACTIVE, "");
        entity.setId(4L);
        entity.setVisibility("private");
        entity.setOwnerType("user");
        entity.setOwnerId("101");
        entity.setBizModule("order_attachment");
        entity.setBizId("A001");
        when(fileRecordMapper.findById(4L)).thenReturn(entity);

        AuthContext.set(new AuthUser(101L, "user101", "user", "session-1"));
        try {
            assertEquals("4", service.getAuthorizedDetail("4").getId());
        } finally {
            AuthContext.clear();
        }
    }

    @Test
    void getAuthorizedDetailShouldRejectOtherUserToReadPrivateBizFile() {
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
        FileRecordEntity entity = buildImageRecord("private", CommonStatusConstants.ACTIVE, "");
        entity.setId(5L);
        entity.setVisibility("private");
        entity.setOwnerType("user");
        entity.setOwnerId("202");
        when(fileRecordMapper.findById(5L)).thenReturn(entity);

        AuthContext.set(new AuthUser(101L, "user101", "user", "session-2"));
        try {
            assertThrows(BusinessException.class, () -> service.getAuthorizedDetail("5"));
        } finally {
            AuthContext.clear();
        }
    }

    @Test
    void createAuthorizedDownloadUrlShouldReturnBizDownloadPathForLocalPublicFile() {
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
        FileRecordEntity entity = buildImageRecord("public", CommonStatusConstants.ACTIVE, "/api/file/public/public/file/aa/hash.png");
        entity.setId(6L);
        entity.setVisibility("public");
        entity.setOwnerType("user");
        entity.setOwnerId("303");
        when(fileRecordMapper.findById(6L)).thenReturn(entity);

        AuthContext.set(new AuthUser(303L, "user303", "user", "session-3"));
        try {
            assertEquals("/api/file/biz/6/download", service.createAuthorizedDownloadUrl("6").getDownloadUrl());
        } finally {
            AuthContext.clear();
        }
    }

    private FileRecordEntity buildImageRecord(String bucketType, String status, String fileUrl) {
        FileRecordEntity entity = new FileRecordEntity();
        entity.setId(1L);
        entity.setBucketType(bucketType);
        entity.setVisibility(bucketType);
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
