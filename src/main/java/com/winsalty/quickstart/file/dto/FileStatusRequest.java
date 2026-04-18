package com.winsalty.quickstart.file.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class FileStatusRequest {
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
