package com.winsalty.quickstart.infra.es;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Elasticsearch 通用操作实现。
 * 基于 Spring Data Elasticsearch 封装项目常用索引和文档操作。
 * 创建日期：2026-04-23
 * author：sunshengxian
 */
@Service
public class ElasticsearchTemplateServiceImpl implements ElasticsearchTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchTemplateServiceImpl.class);

    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchTemplateServiceImpl(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public boolean indexExists(String indexName) {
        boolean exists = indexOps(indexName).exists();
        log.info("elasticsearch index exists checked, indexName={}, exists={}", indexName, exists);
        return exists;
    }

    @Override
    public boolean createIndex(String indexName) {
        IndexOperations indexOperations = indexOps(indexName);
        if (indexOperations.exists()) {
            // 索引已存在时按成功返回，方便初始化脚本和应用启动重复执行。
            log.info("elasticsearch index create skipped, indexName={}, reason=exists", indexName);
            return true;
        }
        boolean created = indexOperations.create();
        log.info("elasticsearch index created, indexName={}, created={}", indexName, created);
        return created;
    }

    @Override
    public boolean deleteIndex(String indexName) {
        boolean deleted = indexOps(indexName).delete();
        log.info("elasticsearch index deleted, indexName={}, deleted={}", indexName, deleted);
        return deleted;
    }

    @Override
    public String save(String indexName, String id, Object document) {
        IndexQueryBuilder builder = new IndexQueryBuilder().withObject(document);
        if (StringUtils.hasText(id)) {
            // 外部传入 ID 时用于覆盖写或幂等写入，不传则交给 Elasticsearch 自动生成。
            builder.withId(id);
        }
        String documentId = elasticsearchOperations.index(builder.build(), coordinates(indexName));
        log.info("elasticsearch document saved, indexName={}, documentId={}", indexName, documentId);
        return documentId;
    }

    @Override
    public List<String> bulkSave(String indexName, Collection<? extends EsDocument> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            log.info("elasticsearch bulk save skipped, indexName={}, reason=empty", indexName);
            return new ArrayList<String>();
        }
        List<IndexQuery> queries = new ArrayList<IndexQuery>();
        for (EsDocument document : documents) {
            // 批量写入要求业务文档提供稳定 ID，避免重复同步时产生多份索引文档。
            queries.add(new IndexQueryBuilder()
                    .withId(document.getId())
                    .withObject(document)
                    .build());
        }
        List<IndexedObjectInformation> indexedObjects = elasticsearchOperations.bulkIndex(queries, coordinates(indexName));
        List<String> documentIds = new ArrayList<String>();
        for (IndexedObjectInformation indexedObject : indexedObjects) {
            documentIds.add(indexedObject.getId());
        }
        log.info("elasticsearch documents bulk saved, indexName={}, size={}", indexName, documentIds.size());
        return documentIds;
    }

    @Override
    public <T> T getById(String indexName, String id, Class<T> clazz) {
        T record = elasticsearchOperations.get(id, clazz, coordinates(indexName));
        log.info("elasticsearch document loaded, indexName={}, documentId={}, hit={}",
                indexName, id, record != null);
        return record;
    }

    @Override
    public <T> SearchHits<T> search(String indexName, Query query, Class<T> clazz) {
        SearchHits<T> searchHits = elasticsearchOperations.search(query, clazz, coordinates(indexName));
        log.info("elasticsearch search completed, indexName={}, totalHits={}",
                indexName, searchHits.getTotalHits());
        return searchHits;
    }

    @Override
    public String deleteById(String indexName, String id) {
        String documentId = elasticsearchOperations.delete(id, coordinates(indexName));
        log.info("elasticsearch document deleted, indexName={}, documentId={}", indexName, documentId);
        return documentId;
    }

    @Override
    public void refresh(String indexName) {
        indexOps(indexName).refresh();
        log.info("elasticsearch index refreshed, indexName={}", indexName);
    }

    private IndexOperations indexOps(String indexName) {
        return elasticsearchOperations.indexOps(coordinates(indexName));
    }

    private IndexCoordinates coordinates(String indexName) {
        return IndexCoordinates.of(indexName);
    }
}
