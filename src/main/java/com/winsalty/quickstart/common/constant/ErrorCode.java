package com.winsalty.quickstart.common.constant;

/**
 * 统一业务错误码定义。
 * 创建日期：2026-04-18
 * author：sunshengxian
 */
public enum ErrorCode {

    REQUEST_PARAM_INVALID(4001, "请求参数校验失败"),
    REQUEST_BIND_INVALID(4002, "请求参数绑定失败"),
    REQUEST_BODY_INVALID(4003, "请求体格式错误"),
    LOGIN_BAD_CREDENTIALS(4004, "用户名或密码错误"),
    ACCOUNT_UNAVAILABLE(4005, "当前账号不可用"),
    ROLE_NOT_ASSIGNED(4006, "当前账号未分配角色"),
    USERNAME_ALREADY_EXISTS(4007, "用户名已存在"),
    QUERY_CODE_ALREADY_EXISTS(4008, "查询编码已存在"),
    MODULE_TYPE_UNSUPPORTED(4009, "模块类型不支持"),
    AUTH_REQUIRED(4010, "未登录或登录已失效"),
    TOKEN_INVALID(4011, "登录令牌无效或已过期"),
    SESSION_INVALID(4012, "当前会话已失效，请重新登录"),
    INVALID_MENU_ID(4013, "存在无效菜单ID"),
    INVALID_ROUTE_CODE(4014, "存在未授权的路由权限码"),
    LOG_STATUS_UPDATE_UNSUPPORTED(4015, "日志模块不支持状态变更"),
    MENU_PARENT_SELF(4016, "父级菜单不能选择自身"),
    MENU_PARENT_NOT_FOUND(4017, "父级菜单不存在"),
    MENU_EXTERNAL_LINK_REQUIRED(4018, "外链菜单必须填写 externalLink"),
    CONFIG_BOOLEAN_INVALID(4019, "布尔配置值不合法"),
    CONFIG_NUMBER_INVALID(4020, "数字配置值不合法"),
    DICT_TYPE_ALREADY_EXISTS(4021, "字典类型已存在"),
    DICT_TYPE_NOT_FOUND(4022, "字典类型不存在"),
    DICT_VALUE_ALREADY_EXISTS(4023, "字典值已存在"),
    PARAM_KEY_ALREADY_EXISTS(4024, "参数键已存在"),
    PARAM_BOOLEAN_INVALID(4025, "布尔参数值不合法"),
    PARAM_NUMBER_INVALID(4026, "数字参数值不合法"),
    ACCESS_DENIED(4030, "无权限访问该资源"),
    REGISTER_DISABLED(4031, "当前环境不开放注册"),
    REGISTER_VERIFY_CODE_INVALID(4032, "邮箱验证码无效"),
    REGISTER_VERIFY_CODE_SEND_FAILED(4033, "邮箱验证码发送失败"),
    AUTH_RATE_LIMITED(4034, "操作过于频繁，请稍后再试"),
    ACCOUNT_LOCKED(4035, "登录失败次数过多，账号已被临时锁定，请稍后再试"),
    USER_NOT_FOUND(4040, "用户不存在"),
    QUERY_NOT_FOUND(4041, "查询配置不存在"),
    SYSTEM_RECORD_NOT_FOUND(4042, "系统记录不存在"),
    ROLE_NOT_FOUND(4043, "角色不存在"),
    FILE_EMPTY(4044, "上传文件不能为空"),
    FILE_TOO_LARGE(4045, "文件大小不能超过 10MB"),
    FILE_TYPE_UNSUPPORTED(4046, "文件类型不支持"),
    FILE_NOT_FOUND(4047, "文件不存在"),
    FILE_RECORD_NOT_FOUND(4048, "文件记录不存在"),
    DIRECTORY_CREATE_FAILED(4049, "上传目录创建失败"),
    FILE_SAVE_FAILED(4050, "文件保存失败"),
    NOTICE_NOT_FOUND(4051, "公告不存在"),
    DEPARTMENT_NOT_FOUND(4052, "部门不存在"),
    DEPARTMENT_PARENT_NOT_FOUND(4053, "父级部门不存在"),
    DEPARTMENT_CODE_ALREADY_EXISTS(4054, "部门编码已存在"),
    DEPARTMENT_PARENT_SELF(4055, "父级部门不能选择自身"),
    ROLE_ASSIGN_REQUIRED(4056, "至少需要分配一个角色"),
    USER_ROLE_ASSIGN_ONLY(4057, "只能为用户分配角色"),
    MENU_RECORD_NOT_FOUND(4058, "菜单记录不存在"),
    INVALID_DEPARTMENT_ID(4059, "部门ID不合法"),
    PARAM_CONFIG_NOT_FOUND(4060, "参数配置不存在"),
    INVALID_ID(4061, "id 不合法");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
