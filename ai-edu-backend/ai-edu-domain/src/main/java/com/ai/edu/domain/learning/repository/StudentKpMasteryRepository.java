package com.ai.edu.domain.learning.repository;

import com.ai.edu.domain.learning.model.entity.StudentKpMastery;
import com.ai.edu.domain.learning.model.valueobject.KpKey;

import java.util.List;
import java.util.Optional;

/**
 * 学生知识点掌握度仓储接口。
 *
 * <p>以 student_id + kp_key（TextbookKP URI）幂等 UPSERT；掌握度取 max 单调更新由
 * 领域实体 {@link StudentKpMastery} 承载，仓储只负责读写。
 */
public interface StudentKpMasteryRepository {

    /**
     * UPSERT 掌握度记录（INSERT ... ON DUPLICATE KEY UPDATE，student_id+kp_key 唯一）。
     */
    StudentKpMastery upsert(StudentKpMastery mastery);

    /**
     * 按学生查全部掌握度（供知识图谱叠加）。
     */
    List<StudentKpMastery> findByStudentId(Long studentId);

    /**
     * 按学生 + 知识点 URI 查单条（读现值后取 max）。
     */
    Optional<StudentKpMastery> findByStudentAndKp(Long studentId, KpKey kpKey);
}
