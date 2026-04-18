package com.winsalty.quickstart.system.controller;

import com.winsalty.quickstart.auth.annotation.AuditLog;
import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dto.SystemConfigSaveRequest;
import com.winsalty.quickstart.system.dto.SystemListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuSaveRequest;
import com.winsalty.quickstart.system.dto.SystemSaveRequest;
import com.winsalty.quickstart.system.dto.SystemStatusRequest;
import com.winsalty.quickstart.system.dto.UserRoleAssignRequest;
import com.winsalty.quickstart.system.service.SystemConfigService;
import com.winsalty.quickstart.system.service.SystemService;
import com.winsalty.quickstart.system.vo.SystemConfigVo;
import com.winsalty.quickstart.system.vo.SystemMenuVo;
import com.winsalty.quickstart.system.vo.SystemRecordVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import javax.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统管理控制器。
 * 聚合通用系统模块、菜单管理、配置管理和用户角色分配接口。
 * 创建日期：2026-04-17
 * author：sunshengxian
 */
@Validated
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;
    private final SystemConfigService systemConfigService;

    public SystemController(SystemService systemService, SystemConfigService systemConfigService) {
        this.systemService = systemService;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 通用列表入口，只允许前端当前支持的 moduleKey，避免任意表名透传到数据层。
     */
    @GetMapping("/{moduleKey}/list")
    public ApiResponse<PageResponse<SystemRecordVo>> list(@PathVariable("moduleKey") @Pattern(regexp = "users|roles|dicts|logs", message = "moduleKey 不合法") String moduleKey,
                                                          @Validated SystemListRequest request) {
        request.setModuleKey(moduleKey);
        return ApiResponse.success("获取成功", systemService.getPage(request));
    }

    /**
     * 通用详情入口，id 对应各模块统一暴露的 record_code。
     */
    @GetMapping("/detail")
    public ApiResponse<SystemRecordVo> detail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", systemService.getDetail(id));
    }

    /**
     * 通用保存入口，内部根据 moduleKey 分发到用户、角色或字典真实表。
     */
    @AuditLog(logType = "operation", code = "system_save", name = "保存系统记录")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<SystemRecordVo> save(@Validated @RequestBody SystemSaveRequest request) {
        return ApiResponse.success("保存成功", systemService.save(request));
    }

    /**
     * 通用状态切换入口。日志模块只读，不允许通过该入口变更状态。
     */
    @AuditLog(logType = "operation", code = "system_status", name = "更新系统记录状态")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/status")
    public ApiResponse<SystemRecordVo> status(@Validated @RequestBody SystemStatusRequest request) {
        return ApiResponse.success("状态已更新", systemService.updateStatus(request));
    }

    /**
     * 系统配置列表，服务层会使用 Redis 缓存。
     */
    @GetMapping("/configs")
    public ApiResponse<List<SystemConfigVo>> configs() {
        return ApiResponse.success("获取成功", systemConfigService.getConfigs());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/configs/save")
    public ApiResponse<SystemConfigVo> saveConfig(@Valid @RequestBody SystemConfigSaveRequest request) {
        return ApiResponse.success("配置已保存", systemConfigService.saveConfig(request));
    }

    /**
     * 菜单树接口，供菜单管理页展示和编辑层级关系。
     */
    @GetMapping("/menus/tree")
    public ApiResponse<List<SystemMenuVo>> menuTree(@Validated SystemMenuListRequest request) {
        return ApiResponse.success("获取成功", systemService.getMenuTree(request));
    }

    /**
     * 保存菜单后会刷新权限 bootstrap 缓存版本。
     */
    @AuditLog(logType = "operation", code = "menu_save", name = "保存菜单")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/menus/save")
    public ApiResponse<SystemMenuVo> saveMenu(@Valid @RequestBody SystemMenuSaveRequest request) {
        return ApiResponse.success("保存成功", systemService.saveMenu(request));
    }

    /**
     * 更新菜单状态后会刷新权限 bootstrap 缓存版本，确保前端重新拉取后生效。
     */
    @AuditLog(logType = "operation", code = "menu_status", name = "更新菜单状态")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/menus/status")
    public ApiResponse<SystemMenuVo> updateMenuStatus(@Validated @RequestBody SystemStatusRequest request) {
        return ApiResponse.success("状态已更新", systemService.updateMenuStatus(request));
    }

    /**
     * 为用户分配角色。角色变化会影响该用户后续 bootstrap 权限结果。
     */
    @AuditLog(logType = "operation", code = "user_assign_roles", name = "分配用户角色")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/assign-roles")
    public ApiResponse<SystemRecordVo> assignUserRoles(@Validated @RequestBody UserRoleAssignRequest request) {
        return ApiResponse.success("角色已分配", systemService.assignUserRoles(request));
    }
}
