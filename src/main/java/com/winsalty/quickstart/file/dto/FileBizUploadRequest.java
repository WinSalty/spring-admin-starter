package com.winsalty.quickstart.file.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 业务模块文件上传请求。
 * 用于给通用文件中心声明业务归属和访问级别，便于后续按业务维度做安全控制。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
public class FileBizUploadRequest {

    @NotBlank(message = "业务模块不能为空")
    @Size(max = 64, message = "业务模块长度不能超过 64")
    @Pattern(regexp = "[a-z0-9_\\-]+", message = "业务模块仅支持小写字母、数字、下划线和中划线")
    private String bizModule;

    @NotBlank(message = "业务主键不能为空")
    @Size(max = 64, message = "业务主键长度不能超过 64")
    private String bizId;

    @NotBlank(message = "可见性不能为空")
    @Pattern(regexp = "public|private", message = "可见性仅支持 public 或 private")
    private String visibility;
}
