package com.winsalty.quickstart.param.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class ParamStatusRequest {
    @NotBlank(message = "id 不能为空")
    private String id;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "active|disabled", message = "状态值不合法")
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
