package com.ai.edu.infrastructure.persistence.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.valueobject.KpKey;
import com.ai.edu.domain.learning.repository.StudentKpMasteryRepository;
import com.ai.edu.infrastructure.persistence.learning.mapper.StudentKpMasteryMapper;
import com.ai.edu.infrastructure.persistence.learning.po.StudentKpMasteryPo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生知识点掌握度仓储实现（PO ↔ 实体桥接，UPSERT 幂等）。
 *
 * <p>取 max 单调更新由领域实体 {@link StudentKpMastery} 承载（applySignal），
 * 仓储只负责按 student_id + kp_key 幂等写入/读取。
 */
@Repository
public class StudentKpMasteryRepositoryImpl implements StudentKpMasteryRepository {

    @Resource
    private StudentKpMasteryMapper studentKpMasteryMapper;

    @Override
    public StudentKpMastery upsert(StudentKpMastery mastery) {
        StudentKpMasteryPo po = StudentKpMasteryPo.from(mastery);
        studentKpMasteryMapper.upsert(po);
        if (po.getId() != null) {
            mastery.setId(po.getId());
        }
        return mastery;
    }

    @Override
    public List<StudentKpMastery> findByStudentId(Long studentId) {
        return StudentKpMasteryPo.toEntityList(studentKpMasteryMapper.selectByStudentId(studentId));
    }

    @Override
    public Optional<StudentKpMastery> findByStudentAndKp(Long studentId, KpKey kpKey) {
        StudentKpMasteryPo po = studentKpMasteryMapper.selectByStudentAndKp(studentId, kpKey.getValue());
        return po != null ? Optional.of(po.toEntity()) : Optional.empty();
    }
}
