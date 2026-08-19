package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;

import java.util.List;
import java.util.Optional;

/**
 * 知识点仓储接口
 */
public interface KgKnowledgePointRepository {

    KgKnowledgePoint save(KgKnowledgePoint knowledgePoint);

    Optional<KgKnowledgePoint> findById(Long id);

    Optional<KgKnowledgePoint> findByUri(String uri);

    List<KgKnowledgePoint> findByUris(List<String> uris);

    /**
     * 按知识点名精确匹配（答疑 label→URI 解析，limit 1 取最先收录）。
     */
    Optional<KgKnowledgePoint> findByLabel(String label);

    /**
     * 按知识点名模糊匹配（答疑 label→URI 解析兜底，limit 1）。
     */
    Optional<KgKnowledgePoint> findByLabelLike(String label);

    /**
     * 按知识点名模糊召回多个候选（LLM 消歧候选列表，limit 10）。
     */
    List<KgKnowledgePoint> findByLabelLikeList(String label);

    /**
     * 批量 UPSERT：按 URI 判断 insert（知识点只插入新增，不更新已有）
     * @return 插入的数量
     */
    int upsert(List<KgKnowledgePoint> knowledgePoints);

    List<KgKnowledgePoint> findByStatus(String status);

    /**
     * 查询与指定小节 URI 列表关联的活跃知识点（用于按 grade 范围隔离）
     */
    List<KgKnowledgePoint> findAllActiveBySectionUris(List<String> sectionUris);

    /**
     * 统计活跃知识点数量（用于对账）
     */
    int countActive();

    void updateStatus(String uri, String status);

}
