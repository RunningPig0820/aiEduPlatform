package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentTopicMastery;
import com.ai.edu.domain.learning.model.valueobject.TopicKey;
import com.ai.edu.domain.learning.repository.StudentTopicMasteryRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.StudentTopicMasteryMapper;
import com.ai.edu.infrastructure.persistence.learning.po.StudentTopicMasteryPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生题型掌握度仓储实现（PO ↔ 实体桥接，UPSERT 幂等）。
 *
 * <p>取 max 单调更新由领域实体 {@link StudentTopicMastery} 承载（applySignal），
 * 仓储只负责按 student_id + topic_key 幂等写入/读取。
 */
@Repository
public class StudentTopicMasteryRepositoryImpl implements StudentTopicMasteryRepository {

    @Resource
    private StudentTopicMasteryMapper studentTopicMasteryMapper;

    @Override
    public StudentTopicMastery upsert(StudentTopicMastery mastery) {
        StudentTopicMasteryPo po = StudentTopicMasteryPo.from(mastery);
        studentTopicMasteryMapper.upsert(po);
        if (po.getId() != null) {
            mastery.setId(po.getId());
        }
        return mastery;
    }

    @Override
    public List<StudentTopicMastery> findByStudentId(Long studentId) {
        return StudentTopicMasteryPo.toEntityList(studentTopicMasteryMapper.selectByStudentId(studentId));
    }

    @Override
    public Optional<StudentTopicMastery> findByStudentAndTopic(Long studentId, TopicKey topicKey) {
        StudentTopicMasteryPo po = studentTopicMasteryMapper.selectByStudentAndTopic(studentId, topicKey.getValue());
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }
}
