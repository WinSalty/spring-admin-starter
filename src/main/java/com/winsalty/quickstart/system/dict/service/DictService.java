package com.winsalty.quickstart.system.dict.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dict.dto.DictDataListRequest;
import com.winsalty.quickstart.system.dict.dto.DictDataSaveRequest;
import com.winsalty.quickstart.system.dict.dto.DictStatusRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeListRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeSaveRequest;
import com.winsalty.quickstart.system.dict.vo.DictDataVo;
import com.winsalty.quickstart.system.dict.vo.DictTypeVo;

/**
 * 字典服务。
 * 提供字典类型、字典项维护和缓存刷新能力。
 * 创建日期：2026-04-25
 * author：sunshengxian
 */
public interface DictService {

    /**
     * 查询字典类型分页列表。
     */
    PageResponse<DictTypeVo> getTypePage(DictTypeListRequest request);

    /**
     * 新增或编辑字典类型。
     */
    DictTypeVo saveType(DictTypeSaveRequest request);

    /**
     * 更新字典类型启停状态。
     */
    DictTypeVo updateTypeStatus(DictStatusRequest request);

    /**
     * 查询字典项分页列表。
     */
    PageResponse<DictDataVo> getDataPage(DictDataListRequest request);

    /**
     * 查询字典项详情。
     */
    DictDataVo getDataDetail(String id);

    /**
     * 新增或编辑字典项。
     */
    DictDataVo saveData(DictDataSaveRequest request);

    /**
     * 更新字典项启停状态。
     */
    DictDataVo updateDataStatus(DictStatusRequest request);

    /**
     * 刷新字典缓存版本。
     */
    Boolean refreshCache();
}
