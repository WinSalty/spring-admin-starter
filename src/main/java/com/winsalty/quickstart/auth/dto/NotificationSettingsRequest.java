package com.winsalty.quickstart.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 当前用户消息通知设置请求。
 * 创建日期：2026-04-19
 * author：sunshengxian
 */
@Data
public class NotificationSettingsRequest {

    @NotNull(message = "账号安全通知不能为空")
    private Boolean notifyAccount;

    @NotNull(message = "系统消息通知不能为空")
    private Boolean notifySystem;

    @NotNull(message = "待办任务通知不能为空")
    private Boolean notifyTodo;
}
