package com.winsalty.quickstart.system.dict.controller;

import com.winsalty.quickstart.common.api.ApiResponse;
import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dict.dto.DictDataListRequest;
import com.winsalty.quickstart.system.dict.dto.DictDataSaveRequest;
import com.winsalty.quickstart.system.dict.dto.DictStatusRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeListRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeSaveRequest;
import com.winsalty.quickstart.system.dict.service.DictService;
import com.winsalty.quickstart.system.dict.vo.DictDataVo;
import com.winsalty.quickstart.system.dict.vo.DictTypeVo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/system/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/types/list")
    public ApiResponse<PageResponse<DictTypeVo>> typeList(@Validated DictTypeListRequest request) {
        return ApiResponse.success("获取成功", dictService.getTypePage(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/types/save")
    public ApiResponse<DictTypeVo> saveType(@Valid @RequestBody DictTypeSaveRequest request) {
        return ApiResponse.success("保存成功", dictService.saveType(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/types/status")
    public ApiResponse<DictTypeVo> typeStatus(@Valid @RequestBody DictStatusRequest request) {
        return ApiResponse.success("状态已更新", dictService.updateTypeStatus(request));
    }

    @GetMapping("/data/list")
    public ApiResponse<PageResponse<DictDataVo>> dataList(@Validated DictDataListRequest request) {
        return ApiResponse.success("获取成功", dictService.getDataPage(request));
    }

    @GetMapping("/data/detail")
    public ApiResponse<DictDataVo> dataDetail(@RequestParam("id") String id) {
        return ApiResponse.success("获取成功", dictService.getDataDetail(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/data/save")
    public ApiResponse<DictDataVo> saveData(@Valid @RequestBody DictDataSaveRequest request) {
        return ApiResponse.success("保存成功", dictService.saveData(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/data/status")
    public ApiResponse<DictDataVo> dataStatus(@Valid @RequestBody DictStatusRequest request) {
        return ApiResponse.success("状态已更新", dictService.updateDataStatus(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cache/refresh")
    public ApiResponse<Boolean> refreshCache() {
        return ApiResponse.success("缓存已刷新", dictService.refreshCache());
    }
}
