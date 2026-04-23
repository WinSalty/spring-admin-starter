package com.winsalty.quickstart.infra.storage;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.winsalty.quickstart.common.constant.ErrorCode;
import com.winsalty.quickstart.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 七牛云对象存储工具类。
 * 封装上传凭证生成、服务端直传和外链地址拼接，业务层不直接依赖七牛 SDK。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Component
public class QiniuObjectStorageUtil {

    private static final String STORAGE_TYPE_QINIU = "qiniu";

    private final QiniuStorageProperties properties;

    public QiniuObjectStorageUtil(QiniuStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * 上传文件流到七牛云空间。
     */
    public ObjectStorageUploadResult upload(InputStream inputStream, String objectKey, String contentType) {
        validateConfig();
        try {
            Configuration configuration = new Configuration(resolveRegion(properties.getRegion()));
            UploadManager uploadManager = new UploadManager(configuration);
            Auth auth = Auth.create(properties.getAccessKey(), properties.getSecretKey());
            String uploadToken = auth.uploadToken(properties.getBucket(), objectKey);
            Response response = uploadManager.put(inputStream, objectKey, uploadToken, null, contentType);
            if (!response.isOK()) {
                throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED);
            }
            ObjectStorageUploadResult result = new ObjectStorageUploadResult();
            result.setStorageType(STORAGE_TYPE_QINIU);
            result.setObjectKey(objectKey);
            result.setFilePath(objectKey);
            result.setFileUrl(buildPublicUrl(objectKey));
            return result;
        } catch (QiniuException exception) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_UPLOAD_FAILED, "七牛云上传失败");
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
     * 校验七牛云必填配置，避免缺少密钥时发起远端请求。
     */
    public void validateConfig() {
        if (!StringUtils.hasText(properties.getAccessKey())
                || !StringUtils.hasText(properties.getSecretKey())
                || !StringUtils.hasText(properties.getBucket())
                || !StringUtils.hasText(properties.getDomain())) {
            throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID);
        }
    }

    private Region resolveRegion(String region) {
        if (!StringUtils.hasText(region) || "auto".equalsIgnoreCase(region)) {
            return Region.autoRegion();
        }
        if ("huadong".equalsIgnoreCase(region)) {
            return Region.huadong();
        }
        if ("huabei".equalsIgnoreCase(region)) {
            return Region.huabei();
        }
        if ("huanan".equalsIgnoreCase(region)) {
            return Region.huanan();
        }
        if ("beimei".equalsIgnoreCase(region)) {
            return Region.beimei();
        }
        if ("xinjiapo".equalsIgnoreCase(region)) {
            return Region.xinjiapo();
        }
        throw new BusinessException(ErrorCode.OBJECT_STORAGE_CONFIG_INVALID, "七牛云存储区域配置不合法");
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
