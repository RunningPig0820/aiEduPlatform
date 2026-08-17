package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.DerivedKpObs;
import com.ai.edu.domain.learning.model.valueobject.DerivedKpStatus;

import java.util.List;

/**
 * 个体派生观测仓储接口。
 *
 * <p>以 student_id + topic_label + kp_uri 幂等去重（UNIQUE uk_student_topic_kp）；
 * 同生同题型同知识点再次命中 SHALL 递增 occurrence_count，不重复建行。
 * PENDING 观测（kp_uri 为空）不参与 UNIQUE 约束，去重在仓储实现内处理。
 */
public interface DerivedKpObsRepository {

    /**
     * UPSERT 观测：存在则 occurrence_count +1，不存在则插入（幂等去重）。
     */
    DerivedKpObs upsert(DerivedKpObs obs);

    /**
     * 按学生查全部观测（供学生端图谱疑似节点叠加）。
     */
    List<DerivedKpObs> findByStudentId(Long studentId);

    /**
     * 按题型 label 查观测（供聚合任务统计）。
     */
    List<DerivedKpObs> findByTopicLabel(String topicLabel);

    /**
     * 查全部已解析观测（kp_uri 非空，供聚合任务扫描）。
     */
    List<DerivedKpObs> findResolved();

    /**
     * 按多题型 label 查已解析观测（kp_uri 非空；聚合变体合并按 canonical+别名 union 重建）。
     */
    List<DerivedKpObs> findResolvedByTopicLabels(java.util.Collection<String> topicLabels);

    /**
     * 按状态查观测（供维护任务扫描 WEAK/CONFLICTED）。
     */
    List<DerivedKpObs> findByStatus(DerivedKpStatus status);

    /**
     * 更新观测状态（重判转正 / 转人工）。
     */
    void updateStatus(Long id, DerivedKpStatus status);

    /**
     * 统计同题型同知识点的去重学生数（第二信号共现检测）。
     */
    int countDistinctStudentsByTopicAndKp(String topicLabel, String kpUri);

    /**
     * 人工确认挂起观测归属：更新 kp_uri + source=curated + status=RESOLVED。
     *
     * @return 影响行数（0 表示观测不存在或已处理）
     */
    int confirm(Long id, String kpUri);

    /**
     * 学生澄清投票转正：把该生该题型的 PENDING 观测更新为 RESOLVED
     * （kp_uri + source=student_vote + confidence），使待确认清单即时消失。
     *
     * @return 影响行数（0 表示无 PENDING 观测，调用方应新建 RESOLVED 观测）
     */
    int resolvePendingByStudentTopic(Long studentId, String topicLabel, String kpUri, int confidence);

    /**
     * 存疑挂起：该生该题型无 PENDING 观测时插入一条（去重防刷屏，kp_uri 为空不参与唯一约束）。
     * 供 analyze-question 存疑结果持久化，学生选择/后续维护任务补充。
     *
     * @return 影响行数（0 表示已存在 PENDING 观测，幂等）
     */
    int upsertPendingIfAbsent(Long studentId, String topicLabel, Integer grade);

    /**
     * 存疑重判转 WEAK：更新 kp_uri + source=llm + status=WEAK（冷启动弱确定，待第二信号共现转正，不直接 RESOLVED 防幻觉污染）。
     *
     * @return 影响行数
     */
    int resolveWeakByMaintenance(Long id, String kpUri, int confidence);
}
