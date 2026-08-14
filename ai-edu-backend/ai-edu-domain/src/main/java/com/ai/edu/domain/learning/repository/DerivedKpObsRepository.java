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
}
