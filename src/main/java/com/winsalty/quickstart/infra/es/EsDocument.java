package com.winsalty.quickstart.infra.es;

/**
 * Elasticsearch 批量写入文档契约。
 * 业务文档实现该接口后即可复用通用 bulkSave 能力。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface EsDocument {

    String getId();
}
