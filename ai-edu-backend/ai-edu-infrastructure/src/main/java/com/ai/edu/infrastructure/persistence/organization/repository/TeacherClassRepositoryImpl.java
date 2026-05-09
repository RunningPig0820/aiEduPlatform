package com.ai.edu.infrastructure.persistence.organization.repository;

import com.ai.edu.domain.organization.model.entity.TeacherClass;
import com.ai.edu.domain.organization.model.valueobject.TeacherClassStatus;
import com.ai.edu.domain.organization.repository.TeacherClassRepository;
import com.ai.edu.domain.shared.valueobject.ClassId;
import com.ai.edu.domain.shared.valueobject.UserId;
import com.ai.edu.infrastructure.persistence.organization.mapper.TeacherClassMapper;
import com.ai.edu.infrastructure.persistence.organization.po.TeacherClassPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 教师-班级关联仓储实现
 * 实现 Entity 和 PO 的转换
 */
@Repository
public class TeacherClassRepositoryImpl implements TeacherClassRepository {

    @Resource
    private TeacherClassMapper teacherClassMapper;

    @Override
    public TeacherClass save(TeacherClass teacherClass) {
        TeacherClassPO po = toPO(teacherClass);

        if (teacherClass.getId() == null) {
            teacherClassMapper.insert(po);
            teacherClass.setId(po.getId());
        } else {
            teacherClassMapper.updateById(po);
        }
        return teacherClass;
    }

    @Override
    public Optional<TeacherClass> findById(Long id) {
        TeacherClassPO po = teacherClassMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public Optional<TeacherClass> findByTeacherIdAndClassId(UserId teacherId, ClassId classId) {
        TeacherClassPO po = teacherClassMapper.selectByTeacherIdAndClassId(teacherId.getValue(), classId.getValue())
                .orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<TeacherClass> findByClassId(ClassId classId) {
        List<TeacherClassPO> poList = teacherClassMapper.selectByClassId(classId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<TeacherClass> findActiveByClassId(ClassId classId) {
        List<TeacherClassPO> poList = teacherClassMapper.selectActiveByClassId(classId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<TeacherClass> findByTeacherId(UserId teacherId) {
        List<TeacherClassPO> poList = teacherClassMapper.selectByTeacherId(teacherId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public List<TeacherClass> findActiveByTeacherId(UserId teacherId) {
        List<TeacherClassPO> poList = teacherClassMapper.selectActiveByTeacherId(teacherId.getValue());
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public Optional<TeacherClass> findHeadTeacherByClassId(ClassId classId) {
        TeacherClassPO po = teacherClassMapper.selectHeadTeacherByClassId(classId.getValue()).orElse(null);
        return Optional.ofNullable(po).map(this::toEntity);
    }

    @Override
    public List<TeacherClass> findByTeacherIdAndSubject(UserId teacherId, String subject) {
        List<TeacherClassPO> poList = teacherClassMapper.selectByTeacherIdAndSubject(teacherId.getValue(), subject);
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public boolean existsByTeacherIdAndClassId(UserId teacherId, ClassId classId) {
        return teacherClassMapper.existsByTeacherIdAndClassId(teacherId.getValue(), classId.getValue());
    }

    @Override
    public boolean existsHeadTeacherByClassId(ClassId classId) {
        return teacherClassMapper.existsHeadTeacherByClassId(classId.getValue());
    }

    @Override
    public int countByClassIdAndStatus(ClassId classId, TeacherClassStatus status) {
        return teacherClassMapper.countByClassIdAndStatus(classId.getValue(), status.getValue());
    }

    @Override
    public void deleteById(Long id) {
        teacherClassMapper.deleteById(id);
    }

    @Override
    public void deleteByTeacherId(UserId teacherId) {
        teacherClassMapper.deleteByTeacherId(teacherId.getValue());
    }

    @Override
    public void deleteByClassId(ClassId classId) {
        teacherClassMapper.deleteByClassId(classId.getValue());
    }

    // ==================== 转换方法 ====================

    /**
     * PO 转 Entity
     */
    private TeacherClass toEntity(TeacherClassPO po) {
        TeacherClass tc;
        if (Boolean.TRUE.equals(po.getHeadTeacher())) {
            tc = TeacherClass.createAsHeadTeacher(
                UserId.of(po.getTeacherId()),
                ClassId.of(po.getClassId()),
                po.getSubject()
            );
        } else {
            tc = TeacherClass.create(
                UserId.of(po.getTeacherId()),
                ClassId.of(po.getClassId()),
                po.getSubject()
            );
        }

        tc.setId(po.getId());

        if (po.getStatus() != null && "INACTIVE".equals(po.getStatus())) {
            tc.deactivate();
        }

        return tc;
    }

    /**
     * Entity 转 PO
     */
    private TeacherClassPO toPO(TeacherClass teacherClass) {
        TeacherClassPO po = new TeacherClassPO();

        po.setId(teacherClass.getId());
        po.setTeacherId(teacherClass.getTeacherIdValue());
        po.setClassId(teacherClass.getClassIdValue());
        po.setSubject(teacherClass.getSubject());
        po.setHeadTeacher(teacherClass.isHeadTeacher());
        po.setStartDate(teacherClass.getStartDate());
        po.setEndDate(teacherClass.getEndDate());
        po.setStatus(teacherClass.getStatusValue() != null ? teacherClass.getStatusValue() : "ACTIVE");
        po.setDeleted(teacherClass.isDeleted());

        return po;
    }
}