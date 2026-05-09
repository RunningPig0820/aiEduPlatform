package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.StudentClass;
import com.ai.edu.domain.organization.model.valueobject.StudentClassStatus;
import com.ai.edu.domain.organization.repository.StudentClassRepository;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
import com.ai.edu.infrastructure.persistence.organization.mapper.StudentClassMapper;
import com.ai.edu.infrastructure.persistence.organization.po.StudentClassPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生-班级关联仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Repository
public class StudentClassRepositoryImpl implements StudentClassRepository {

    @Resource
    private StudentClassMapper studentClassMapper;

    @Override
    public StudentClass save(StudentClass studentClass) {
        StudentClassPO po = toPO(studentClass);

        if (studentClass.getId() == null) {
            studentClassMapper.insert(po);
            studentClass.setId(po.getId());
        } else {
            studentClassMapper.updateById(po);
        }
        return studentClass;
    }

    @Override
    public Optional<StudentClass> findById(Long id) {
        StudentClassPO po = studentClassMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<StudentClass> findByStudentIdAndClassId(UserId studentId, ClassId classId) {
        StudentClassPO po = studentClassMapper.selectByStudentIdAndClassId(studentId.getValue(), classId.getValue())
                .orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<StudentClass> findActiveByStudentId(UserId studentId) {
        StudentClassPO po = studentClassMapper.selectActiveByStudentId(studentId.getValue()).orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<StudentClass> findByClassId(ClassId classId) {
        List<StudentClassPO> poList = studentClassMapper.selectByClassId(classId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<StudentClass> findActiveByClassId(ClassId classId) {
        List<StudentClassPO> poList = studentClassMapper.selectActiveByClassId(classId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<StudentClass> findByStudentId(UserId studentId) {
        List<StudentClassPO> poList = studentClassMapper.selectByStudentId(studentId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<StudentClass> findByStatus(StudentClassStatus status) {
        List<StudentClassPO> poList = studentClassMapper.selectByStatus(status.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByStudentIdAndClassId(UserId studentId, ClassId classId) {
        return studentClassMapper.existsByStudentIdAndClassId(studentId.getValue(), classId.getValue());
    }

    @Override
    public int countByClassIdAndStatus(ClassId classId, StudentClassStatus status) {
        return studentClassMapper.countByClassIdAndStatus(classId.getValue(), status.getValue());
    }

    @Override
    public void deleteById(Long id) {
        studentClassMapper.deleteById(id);
    }

    @Override
    public void deleteByStudentId(UserId studentId) {
        studentClassMapper.deleteByStudentId(studentId.getValue());
    }

    @Override
    public void deleteByClassId(ClassId classId) {
        studentClassMapper.deleteByClassId(classId.getValue());
    }

    // ==================== 转换方法 ====================

    /**
     * PO 转 Entity
     */
    private StudentClass toEntity(StudentClassPO po) {
        StudentClass sc = StudentClass.create(
            UserId.of(po.getStudentId()),
            ClassId.of(po.getClassId())
        );

        sc.setId(po.getId());

        if (po.getStudentNo() != null) {
            sc.setStudentNo(po.getStudentNo());
        }

        if (po.getStatus() != null) {
            StudentClassStatus status = StudentClassStatus.of(po.getStatus());
            // 根据状态设置
            if ("ACTIVE".equals(po.getStatus())) {
                sc.activate();
            } else if ("GRADUATED".equals(po.getStatus())) {
                sc.graduate();
            } else if ("TRANSFERRED".equals(po.getStatus())) {
                sc.transfer();
            }
        }

        return sc;
    }

    /**
     * Entity 转 PO
     */
    private StudentClassPO toPO(StudentClass studentClass) {
        StudentClassPO po = new StudentClassPO();

        po.setId(studentClass.getId());
        po.setStudentId(studentClass.getStudentIdValue());
        po.setClassId(studentClass.getClassIdValue());
        po.setStudentNo(studentClass.getStudentNo());
        po.setJoinDate(studentClass.getJoinDate());
        po.setLeaveDate(studentClass.getLeaveDate());
        po.setStatus(studentClass.getStatusValue() != null ? studentClass.getStatusValue() : "ACTIVE");
        po.setDeleted(studentClass.isDeleted());

        return po;
    }
}