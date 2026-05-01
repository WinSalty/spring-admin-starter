package com.winsalty.quickstart.credential.service;

import com.winsalty.quickstart.common.api.PageResponse;
import com.winsalty.quickstart.credential.dto.CredentialBatchListRequest;
import com.winsalty.quickstart.credential.dto.CredentialCategorySaveRequest;
import com.winsalty.quickstart.credential.dto.CredentialGeneratedBatchCreateRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportConfirmRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportPreviewRequest;
import com.winsalty.quickstart.credential.dto.CredentialImportTaskListRequest;
import com.winsalty.quickstart.credential.dto.CredentialItemListRequest;
import com.winsalty.quickstart.credential.dto.CredentialRedeemRecordListRequest;
import com.winsalty.quickstart.credential.vo.CredentialBatchVo;
import com.winsalty.quickstart.credential.vo.CredentialCategoryVo;
import com.winsalty.quickstart.credential.vo.CredentialImportPreviewVo;
import com.winsalty.quickstart.credential.vo.CredentialImportTaskVo;
import com.winsalty.quickstart.credential.vo.CredentialItemVo;
import com.winsalty.quickstart.credential.vo.CredentialRedeemRecordVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 管理端凭证中心服务接口。
 * 定义分类、批次、明细、导入任务和兑换记录的后台管理能力。
 * 创建日期：2026-05-01
 * author：sunshengxian
 */
public interface CredentialAdminService {

    /** 查询凭证分类。 */
    List<CredentialCategoryVo> listCategories();

    /** 创建凭证分类。 */
    CredentialCategoryVo createCategory(CredentialCategorySaveRequest request);

    /** 更新凭证分类。 */
    CredentialCategoryVo updateCategory(Long id, CredentialCategorySaveRequest request);

    /** 停用凭证分类。 */
    CredentialCategoryVo disableCategory(Long id);

    /** 分页查询凭证批次。 */
    PageResponse<CredentialBatchVo> listBatches(CredentialBatchListRequest request);

    /** 查询凭证批次详情。 */
    CredentialBatchVo getBatch(Long id);

    /** 创建系统生成凭证批次。 */
    CredentialBatchVo createGeneratedBatch(CredentialGeneratedBatchCreateRequest request);

    /** 预览文本卡密导入结果。 */
    CredentialImportPreviewVo previewImport(CredentialImportPreviewRequest request);

    /** 确认导入文本卡密批次。 */
    CredentialBatchVo confirmImport(CredentialImportConfirmRequest request);

    /** 停用凭证批次。 */
    CredentialBatchVo disableBatch(Long id);

    /** 分页查询凭证明细。 */
    PageResponse<CredentialItemVo> listItems(CredentialItemListRequest request);

    /** 查看凭证明文。 */
    String revealItem(Long id, HttpServletRequest servletRequest);

    /** 停用凭证明细。 */
    CredentialItemVo disableItem(Long id);

    /** 启用凭证明细。 */
    CredentialItemVo enableItem(Long id);

    /** 分页查询导入任务。 */
    PageResponse<CredentialImportTaskVo> listImportTasks(CredentialImportTaskListRequest request);

    /** 分页查询兑换记录。 */
    PageResponse<CredentialRedeemRecordVo> listRedeemRecords(CredentialRedeemRecordListRequest request);
}
