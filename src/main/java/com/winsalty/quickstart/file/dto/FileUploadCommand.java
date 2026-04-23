package com.winsalty.quickstart.file.dto;

import lombok.Data;

/**
 * 文件上传内部命令对象。
 * 统一承载业务归属、可见性和归属人信息，供控制器和其他业务模块复用文件中心能力。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Data
public class FileUploadCommand {

    /** 业务模块编码。 */
    private String bizModule;
    /** 业务主键。 */
    private String bizId;
    /** 文件可见性：public/private。 */
    private String visibility;
    /** 归属对象类型，例如 user/admin/system。 */
    private String ownerType;
    /** 归属对象ID。 */
    private String ownerId;
    /** 上传业务类型，用于生成对象存储目录。 */
    private String uploadBizType;
}
