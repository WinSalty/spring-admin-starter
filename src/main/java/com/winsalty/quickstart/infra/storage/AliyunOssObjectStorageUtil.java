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

/**
 * 阿里云 OSS 对象存储工具类。
 * 封装服务端直传和外链地址拼接，业务层不直接依赖阿里云 OSS SDK。
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
     * 上传文件流到阿里云 OSS Bucket。
     */
    public ObjectStorageUploadResult upload(InputStream inputStream, String objectKey, String contentType, long sizeBytes) {
        validateConfig();
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
            ossClient.putObject(properties.getBucket(), objectKey, inputStream, metadata);
            ObjectStorageUploadResult result = new ObjectStorageUploadResult();
            result.setStorageType(STORAGE_TYPE_ALIYUN_OSS);
            result.setObjectKey(objectKey);
            result.setFilePath(objectKey);
            result.setFileUrl(buildPublicUrl(objectKey));
            return result;
        } catch (OSSException | ClientException exception) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "阿里云 OSS 上传失败");
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 拼接公开访问地址。
     */
    public String buildPublicUrl(String objectKey) {
        String domain = trimTrailingSlash(properties.getDomain());
        if (!StringUtils.hasText(domain)) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
        return domain + "/" + objectKey;
    }

    /**
     * 校验阿里云 OSS 必填配置，避免缺少密钥时发起远端请求。
     */
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getEndpoint())
                || !StringUtils.hasText(properties.getAccessKeyId())
                || !StringUtils.hasText(properties.getAccessKeySecret())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getDomain())) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
