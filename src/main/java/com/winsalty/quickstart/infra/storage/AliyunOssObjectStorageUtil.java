package com.winsalty.quickstart.infra.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.OSSObject;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
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
public class AliyunOssObjectStorageUtil implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssObjectStorageUtil.class);
    private static final String STORAGE_TYPE_ALIYUN_OSS = "aliyun-oss";
    private static final String DEFAULT_BUCKET_TYPE = "private";
    private static final String PRIVATE_READ_POLICY = "private_read";
    private static final long MILLIS_PER_SECOND = 1000L;

    private final AliyunOssStorageProperties properties;
    private volatile OSS ossClient;

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
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(sizeBytes);
            if (StringUtils.hasText(contentType)) {
                // 保留 MIME 类型，后续代理预览或下载时可按原始类型响应。
                metadata.setContentType(contentType);
            }
            getOssClient().putObject(bucketName, objectKey, inputStream, metadata);
            ObjectStorageUploadResult result = new ObjectStorageUploadResult();
            result.setStorageType(STORAGE_TYPE_ALIYUN_OSS);
            result.setBucketType(StringUtils.hasText(bucketType) ? bucketType : DEFAULT_BUCKET_TYPE);
            result.setBucketName(bucketName);
            result.setAccessPolicy(PRIVATE_READ_POLICY);
            result.setObjectKey(objectKey);
            result.setFilePath(objectKey);
            result.setFileUrl("");
            log.info("aliyun oss private object uploaded, bucketName={}, objectKey={}, sizeBytes={}",
                    bucketName, objectKey, sizeBytes);
            return result;
        } catch (OSSException | ClientException exception) {
            log.error("aliyun oss private object upload failed, bucketName={}, objectKey={}", bucketName, objectKey, exception);
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "阿里云 OSS 上传失败");
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
        try {
            // 私有文件不暴露永久地址，所有下载入口都通过短期签名 URL 控制有效期。
            Date expiration = new Date(System.currentTimeMillis() + expireSeconds * MILLIS_PER_SECOND);
            URL url = getOssClient().generatePresignedUrl(bucketName, objectKey, expiration);
            log.info("aliyun oss presigned url generated, bucketName={}, objectKey={}, expireSeconds={}",
                    bucketName, objectKey, expireSeconds);
            return url.toString();
        } catch (OSSException | ClientException exception) {
            log.error("aliyun oss presigned url generate failed, bucketName={}, objectKey={}", bucketName, objectKey, exception);
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "阿里云 OSS 签名地址生成失败");
        }
    }

    /**
     * 检查私有 Bucket 中对象是否存在，供内容 Hash 复用前校验远端对象有效性。
     *
     * @param bucketName 私有 Bucket 名称
     * @param objectKey 对象 Key
     * @return 对象是否存在
     * @author sunshengxian
     * @date 2026-04-24
     */
    public boolean objectExists(String bucketName, String objectKey) {
        validateBaseConfig();
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
        try {
            boolean exists = getOssClient().doesObjectExist(bucketName, objectKey);
            log.info("aliyun oss object exists checked, bucketName={}, objectKey={}, exists={}",
                    bucketName, objectKey, exists);
            return exists;
        } catch (OSSException | ClientException exception) {
            log.error("aliyun oss object exists check failed, bucketName={}, objectKey={}", bucketName, objectKey, exception);
            return false;
        }
    }

    /**
     * 打开私有 Bucket 中对象的读取流，供后端代理图片等需要同域展示的资源。
     *
     * @param bucketName 私有 Bucket 名称
     * @param objectKey 对象 Key
     * @return 对象内容输入流
     * @author sunshengxian
     * @date 2026-04-24
     */
    public InputStream openObjectStream(String bucketName, String objectKey) {
        validateBaseConfig();
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
        try {
            OSSObject ossObject = getOssClient().getObject(bucketName, objectKey);
            log.info("aliyun oss object stream opened, bucketName={}, objectKey={}", bucketName, objectKey);
            return ossObject.getObjectContent();
        } catch (OSSException | ClientException exception) {
            log.error("aliyun oss object stream open failed, bucketName={}, objectKey={}", bucketName, objectKey, exception);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND, "阿里云 OSS 文件读取失败");
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

    private OSS getOssClient() {
        if (ossClient != null) {
            return ossClient;
        }
        synchronized (this) {
            if (ossClient == null) {
                validateBaseConfig();
                // OSS Client 复用连接池，避免每次上传或签名都新建客户端。
                ossClient = new OSSClientBuilder().build(
                        properties.getEndpoint(),
                        properties.getAccessKeyId(),
                        properties.getAccessKeySecret()
                );
            }
        }
        return ossClient;
    }

    @Override
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("aliyun oss client shutdown");
        }
    }
}
