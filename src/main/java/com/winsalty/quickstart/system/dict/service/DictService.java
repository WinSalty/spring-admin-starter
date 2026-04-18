package com.winsalty.quickstart.system.dict.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.system.dict.dto.DictDataListRequest;
import com.winsalty.quickstart.system.dict.dto.DictDataSaveRequest;
import com.winsalty.quickstart.system.dict.dto.DictStatusRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeListRequest;
import com.winsalty.quickstart.system.dict.dto.DictTypeSaveRequest;
import com.winsalty.quickstart.system.dict.vo.DictDataVo;
import com.winsalty.quickstart.system.dict.vo.DictTypeVo;

public interface DictService {
    PageResponse<DictTypeVo> getTypePage(DictTypeListRequest request);

    DictTypeVo saveType(DictTypeSaveRequest request);

    DictTypeVo updateTypeStatus(DictStatusRequest request);

    PageResponse<DictDataVo> getDataPage(DictDataListRequest request);

    DictDataVo getDataDetail(String id);

    DictDataVo saveData(DictDataSaveRequest request);

    DictDataVo updateDataStatus(DictStatusRequest request);

    Boolean refreshCache();
}
