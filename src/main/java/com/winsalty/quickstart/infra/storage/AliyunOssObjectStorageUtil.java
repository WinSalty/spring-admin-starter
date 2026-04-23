package com.winsalty.quickstart.infra.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储工具类。
 * 统一使用私有 Bucket 保存文件，访问时由后端生成短期签名 URL。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Component
public class AliyunOssObjectStorageUtil {

    private static final String STORAGE_TYPE_ALIYUN_OSS = "aliyun-oss";

    private final AliyunOssStorageProperties properties;

    public AliyunOssObjectStorageUtil(AliyunOssStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 上传私有业务文件到阿里云 OSS 私有 Bucket。
     *
     * @param inputStream 文件输入流
     * @param objectKey 对象 Key
     * @param contentType 文件 MIME
     * @param sizeBytes 文件大小
     * @param bucketType 业务文件类型
     * @return 上传结果
     * @author sunshengxian
     * @date 2026-04-23
     */
    public ObjectStorageUploadResult uploadPrivate(InputStream inputStream,
                                                   String objectKey,
                                                   String contentType,
                                                   long sizeBytes,
                                                   String bucketType) {
        validatePrivateConfig();
        return uploadToPrivateBucket(objectKey, contentType, sizeBytes, inputStream, bucketType);
    }

    private ObjectStorageUploadResult uploadToPrivateBucket(String objectKey,
                                                            String contentType,
                                                            long sizeBytes,
                                                            InputStream inputStream,
                                                            String bucketType) {
        String bucketName = properties.getPrivateBucket();
        OSS ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(sizeBytes);
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(bucketName, objectKey, inputStream, metadata);
            ObjectStorageUploadResult result = new ObjectStorageUploadResult();
            result.setStorageType(STORAGE_TYPE_ALIYUN_OSS);
            result.setBucketType(StringUtils.hasText(bucketType) ? bucketType : "private");
            result.setBucketName(bucketName);
            result.setAccessPolicy("private_read");
            result.setObjectKey(objectKey);
            result.setFilePath(objectKey);
            result.setFileUrl("");
            return result;
        } catch (OSSException | ClientException exception) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "阿里云 OSS 上传失败");
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 为私有文件生成临时下载地址。
     *
     * @param bucketName 私有 Bucket 名称
     * @param objectKey 对象 Key
     * @param expireSeconds 有效秒数
     * @return 临时签名 URL
     * @author sunshengxian
     * @date 2026-04-23
     */
    public String generatePresignedUrl(String bucketName, String objectKey, long expireSeconds) {
        validateBaseConfig();
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
        OSS ossClient = new OSSClientBuilder().build(
                properties.getEndpoint(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret()
        );
        try {
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);
            URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
            return url.toString();
        } catch (OSSException | ClientException exception) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "阿里云 OSS 签名地址生成失败");
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 校验阿里云 OSS 必填配置，避免缺少密钥时发起远端请求。
     */
    public void validateConfig() {
        validatePrivateConfig();
    }

    public void validatePrivateConfig() {
        validateBaseConfig();
        if (!StringUtils.hasText(properties.getPrivateBucket())) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
    }

    private void validateBaseConfig() {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
    }
}
