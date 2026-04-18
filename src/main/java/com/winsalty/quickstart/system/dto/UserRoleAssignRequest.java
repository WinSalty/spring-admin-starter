package com.winsalty.quickstart.system.dto;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户角色分配请求对象。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public class UserRoleAssignRequest {

    @NotBlank(message = "用户ID不能为空")
    private String userId;

    private List<String> roleCodes = new ArrayList<String>();

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }
}
