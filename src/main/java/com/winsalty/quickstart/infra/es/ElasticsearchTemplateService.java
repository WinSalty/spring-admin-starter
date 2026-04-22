package com.winsalty.quickstart.infra.es;

import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.Collection;
import java.util.List;

/**
 * Elasticsearch 通用操作接口。
 * 封装索引管理、单文档写入、批量写入、查询和删除等常用能力。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
public interface ElasticsearchTemplateService {

    boolean indexExists(String indexName);

    boolean createIndex(String indexName);

    boolean deleteIndex(String indexName);

    String save(String indexName, String id, Object document);

    List<String> bulkSave(String indexName, Collection<? extends EsDocument> documents);

    <T> T getById(String indexName, String id, Class<T> clazz);

    <T> SearchHits<T> search(String indexName, Query query, Class<T> clazz);

    String deleteById(String indexName, String id);

    void refresh(String indexName);
}
