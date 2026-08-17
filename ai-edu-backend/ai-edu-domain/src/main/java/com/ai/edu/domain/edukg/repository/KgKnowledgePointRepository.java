package com.ai.edu.domain.edukg.repository;

import com.ai.edu.domain.edukg.model.entity.KgKnowledgePoint;
import com.ai.edu.domain.edukg.model.valueobject.KgKpPlacement;

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

    /**
     * 批量反查知识点归属（kp→section→chapter→textbook 的 stage/chapter/section 投影）。
     * 供掌握度 stage 字段与知识点总览分页复用；一个 kp 挂多个 section 时取首个非空 stage。
     */
    List<KgKpPlacement> findPlacementByUris(List<String> kpUris);

    /**
     * 按学段分页列教材知识点（带章节/小节归属），供知识点总览。
     */
    List<KgKpPlacement> findPageByStage(String stage, int offset, int limit);

    /**
     * 某学段教材知识点总数（分页 total）。
     */
    long countByStage(String stage);

    /**
     * 按学段取全部知识点 label（去重，封闭域约束选择的候选池，供 analyze 池选择）。
     */
    List<String> findLabelsByStage(String stage);

    /**
     * 按学段 + 关键词分页列知识点（label LIKE，前端 KpSearchSelector 空候选搜索兜底）。
     */
    List<KgKpPlacement> findPageByStageAndKeyword(String stage, String keyword, int offset, int limit);

    /**
     * 某学段 + 关键词知识点总数（分页 total）。
     */
    long countByStageAndKeyword(String stage, String keyword);
}
