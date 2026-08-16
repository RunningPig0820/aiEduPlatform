package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;

import java.util.List;
import java.util.Optional;

/**
 * 学生题型掌握度仓储接口（掌握度主体翻转后新增）。
 *
 * <p>以 student_id + topic_key（归一化题型名）幂等 UPSERT；掌握度取 max 单调更新由
 * 领域实体 {@link StudentTopicMastery} 承载，仓储只负责读写。
 */
public interface StudentTopicMasteryRepository {

    /**
     * UPSERT 掌握度记录（INSERT ... ON DUPLICATE KEY UPDATE，student_id+topic_key 唯一）。
     */
    StudentTopicMastery upsert(StudentTopicMastery mastery);

    /**
     * 按学生查全部题型掌握度（供掌握度列表 + 知识点覆盖度派生）。
     */
    List<StudentTopicMastery> findByStudentId(Long studentId);

    /**
     * 按学生 + 题型标识查单条（读现值后取 max）。
     */
    Optional<StudentTopicMastery> findByStudentAndTopic(Long studentId, TopicKey topicKey);
}
