package com.winsalty.quickstart.system.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dto.SystemConfigSaveRequest;
import com.winsalty.quickstart.system.dto.SystemListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuListRequest;
import com.winsalty.quickstart.system.dto.SystemMenuSaveRequest;
import com.winsalty.quickstart.system.dto.SystemSaveRequest;
import com.winsalty.quickstart.system.dto.SystemStatusRequest;
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

    @GetMapping("/{moduleKey}/list")
    public ApiResponse<PageResponse<SystemRecordVo>> list(@PathVariable("moduleKey") @Pattern(regexp = "users|roles|dicts|logs", message = "moduleKey 不合法") String moduleKey,
                                                          @Validated SystemListRequest request) {
        request.setModuleKey(moduleKey);
        return ApiResponse.success("获取成功", systemService.getPage(request));
    }

    @GetMapping("/detail")
    public ApiResponse<SystemRecordVo> detail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", systemService.getDetail(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public ApiResponse<SystemRecordVo> save(@Validated @RequestBody SystemSaveRequest request) {
        return ApiResponse.success("保存成功", systemService.save(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/status")
    public ApiResponse<SystemRecordVo> status(@Validated @RequestBody SystemStatusRequest request) {
        return ApiResponse.success("状态已更新", systemService.updateStatus(request));
    }

    @GetMapping("/configs")
    public ApiResponse<List<SystemConfigVo>> configs() {
        return ApiResponse.success("获取成功", systemConfigService.getConfigs());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/configs/save")
    public ApiResponse<SystemConfigVo> saveConfig(@Valid @RequestBody SystemConfigSaveRequest request) {
        return ApiResponse.success("配置已保存", systemConfigService.saveConfig(request));
    }

    @GetMapping("/menus/tree")
    public ApiResponse<List<SystemMenuVo>> menuTree(@Validated SystemMenuListRequest request) {
        return ApiResponse.success("获取成功", systemService.getMenuTree(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/menus/save")
    public ApiResponse<SystemMenuVo> saveMenu(@Valid @RequestBody SystemMenuSaveRequest request) {
        return ApiResponse.success("保存成功", systemService.saveMenu(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/menus/status")
    public ApiResponse<SystemMenuVo> updateMenuStatus(@Validated @RequestBody SystemStatusRequest request) {
        return ApiResponse.success("状态已更新", systemService.updateMenuStatus(request));
    }
}
