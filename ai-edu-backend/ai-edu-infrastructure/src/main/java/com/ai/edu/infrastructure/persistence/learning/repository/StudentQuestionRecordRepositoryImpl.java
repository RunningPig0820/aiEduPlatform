package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentQuestionRecord;
import com.ai.edu.domain.learning.repository.StudentQuestionRecordRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.StudentQuestionRecordMapper;
import com.ai.edu.infrastructure.persistence.learning.po.StudentQuestionRecordPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 学生题目记录仓储实现（PO ↔ 实体桥接，INSERT 一条作答一条）。
 *
 * <p>掌握度事实源：题目证据全量落库，掌握表聚合结果可由本表信号重算（改折扣系数/信号映射不丢证据）。
 */
@Repository
public class StudentQuestionRecordRepositoryImpl implements StudentQuestionRecordRepository {

    @Resource
    private StudentQuestionRecordMapper studentQuestionRecordMapper;

    @Override
    public StudentQuestionRecord save(StudentQuestionRecord record) {
        StudentQuestionRecordPo po = StudentQuestionRecordPo.from(record);
        studentQuestionRecordMapper.insert(po);
        record.setId(po.getId());
        return record;
    }

    @Override
    public List<StudentQuestionRecord> findByStudentId(Long studentId) {
        return StudentQuestionRecordPo.toEntityList(studentQuestionRecordMapper.selectByStudentId(studentId));
    }

    @Override
    public List<StudentQuestionRecord> findByStudentAndCanonical(Long studentId, String canonicalLabel) {
        return StudentQuestionRecordPo.toEntityList(
                studentQuestionRecordMapper.selectByStudentAndCanonical(studentId, canonicalLabel));
    }

    @Override
    public List<String> findPendingTopicLabels() {
        return studentQuestionRecordMapper.selectPendingTopicLabels();
    }

    @Override
    public List<String> findPendingTopicLabelsByStudent(Long studentId) {
        return studentQuestionRecordMapper.selectPendingTopicLabelsByStudent(studentId);
    }

    @Override
    public int updateCanonicalByTopic(String topicLabel, String canonicalLabel) {
        return studentQuestionRecordMapper.updateCanonicalByTopic(topicLabel, canonicalLabel);
    }

    @Override
    public List<StudentQuestionRecord> findAll() {
        return StudentQuestionRecordPo.toEntityList(studentQuestionRecordMapper.selectAll());
    }
}